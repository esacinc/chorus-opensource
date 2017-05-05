package com.infoclinika.integration.skyline;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.infoclinika.msdata.image.*;
import com.infoclinika.msdata.server.api.ChunkRange;
import com.infoclinika.msdata.server.api.RawDataAccessService;
import com.infoclinika.msdata.server.impl.PerFileRawDataAccessService;
import com.infoclinika.msdata.services.MS1DataService;
import com.infoclinika.msexplorer.messaging.InvalidTaskResultException;
import com.infoclinika.tasks.api.workflow.ChroExtractionTaskId;
import com.infoclinika.tasks.api.workflow.ChromatogramSource;
import com.infoclinika.tasks.api.workflow.input.ChroExtractionTask;
import com.infoclinika.tasks.api.workflow.input.CompositeChroExtractionTask;
import com.infoclinika.tasks.api.workflow.output.ChroExtractionTaskResult;
import com.infoclinika.tasks.api.workflow.output.CompositeRawChroExtractionResult;
import com.infoclinika.tasks.api.workflow.output.RawChroExtractionResult;
import computations.MessagingComputationsClient;
import computations.impl.ChroExtractionClient;
import computations.impl.MessagingSerialization;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @author Oleksii Tymchenko
 */
public class SkylineGroupingExtractionClient implements MessagingComputationsClient {

    private final static Logger LOGGER = Logger.getLogger(SkylineGroupingExtractionClient.class);

    private final ChroExtractionClient client;

    private final Map<ChroExtractionTaskId, ExtractionTaskAndResult> originalTasks = Collections.synchronizedMap(new HashMap<>());
    private final String sharedStoragePath;

    public SkylineGroupingExtractionClient(ChroExtractionClient client, String sharedStoragePath) {
        this.client = client;
        this.sharedStoragePath = sharedStoragePath;
    }

    @Override
    public ChroExtractionTaskResult extract(ChroExtractionTask task) {
        throw new IllegalStateException("Standard extraction must not be used for Skyline raw extraction. Use raw extraction instead.");
    }


    @Override
    public RawChroExtractionResult extractRawGrouped(CompositeChroExtractionTask task) {
        throw new IllegalStateException("Grouping extraction must not be used for Skyline raw extraction. Use raw extraction instead.");
    }

    @Override
    public RawChroExtractionResult extractRaw(ChroExtractionTask task) {
        if(task.getSources().size() != 1) {
            throw new IllegalArgumentException("Tasks with multiple sources cannot be extracted with Skyline extractor. Task: " + task);
        }
        final ChroExtractionTaskId id = ChroExtractionTaskId.toId(task);
        originalTasks.put(id, new ExtractionTaskAndResult(task));
        return new RawChroExtractionResult(null) {
            @Override
            public String getSerializedChroExtractionResult() {
                final ExtractionTaskAndResult extractionTaskAndResult = originalTasks.get(id);
                if(extractionTaskAndResult == null) {
                    final String message = "Cannot obtain serialized chro extraction result for task: " + task;
                    LOGGER.error(message);
                    throw new RuntimeException(message);
                }
                return extractionTaskAndResult.result;
            }

            @Override
            public void setSerializedChroExtractionResult(String serializedChroExtractionResult) {
                super.setSerializedChroExtractionResult(serializedChroExtractionResult);
            }
        };
    }

    public void groupAndSendAllWith(ExecutorService executorService) {
        final List<ChroExtractionTask> originalTaskList = newArrayList(Iterables.transform(originalTasks.values(), (taskAndId) -> taskAndId.task));
        final List<CompositeChroExtractionTask> groupedTasks = groupTasksPerChunk(executorService, originalTaskList);

        final ExecutionContextHolder executionContextHolder = composeJobs(groupedTasks);
        executeAndWire(executorService, executionContextHolder.getJobs());


        final Set<CompositeChroExtractionTask> tasksToRetry = executionContextHolder.getTasksToRetry();
        //retrying tasks...
        for (CompositeChroExtractionTask composite : tasksToRetry) {
            for (ChroExtractionTask task : composite.getTasks()) {
                //reset their start time
                task.setStartTimeMillisUtc(System.currentTimeMillis());
            }
        }

        final List<CompositeChroExtractionTask> retryList = new ArrayList<>();
        retryList.addAll(tasksToRetry);
        final ExecutionContextHolder retryHolder = composeJobs(retryList);
        executeAndWire(executorService, retryHolder.getJobs());

        final Set<CompositeChroExtractionTask> finalFailedTasks = retryHolder.getTasksToRetry();
        if(!finalFailedTasks.isEmpty()) {
            final StringBuilder errorBuilder = new StringBuilder();
            errorBuilder.append("Tasks failed to process even after retry: ");
            for (CompositeChroExtractionTask failed : finalFailedTasks) {
                errorBuilder.append("\n  + ").append(failed);
            }
            final String message = " Some tasks failed to process even after retry: " + errorBuilder.toString();
            LOGGER.error(message);
            throw new RuntimeException(message);
        }
    }

    private ExecutionContextHolder composeJobs(List<CompositeChroExtractionTask> groupedTasks) {

        final List<Callable<CompositeRawChroExtractionResult>> rawExtractionJobs = new LinkedList<>();
        final Set<CompositeChroExtractionTask> tasksToRetry = Collections.synchronizedSet(new HashSet<>());

        for (CompositeChroExtractionTask groupedTask : groupedTasks) {
            rawExtractionJobs.add(() -> {
                final long start = System.currentTimeMillis();
                RawChroExtractionResult rawResponse = null;
                try {
                    rawResponse = client.extractRawGrouped(groupedTask);
                    final String serializedResult = rawResponse.getSerializedChroExtractionResult();
                    final CompositeRawChroExtractionResult result = MessagingSerialization.deserialize(serializedResult, CompositeRawChroExtractionResult.class);
                    final long elapsed = System.currentTimeMillis() - start;
                    final ChroExtractionTask firstSingleTask = groupedTask.getTasks().get(0);
                    LOGGER.info("[Grouped " + elapsed + "ms] Extracted for group. Group size: " + groupedTask.getTasks().size() +". Chunk = "+groupedTask.getChunkIndex()+". Target filter = " + firstSingleTask.getSources().get(0));
                    return result;
                } catch(InvalidTaskResultException invalidTaskException) {
                    LOGGER.warn("Invalid result has been received for task. Retrying. Task = " + groupedTask, invalidTaskException);
                    tasksToRetry.add(groupedTask);
                    return null;
                } catch (Exception e) {
                    if(rawResponse == null) {
                        LOGGER.warn("Raw response is null for task. Retrying. Task = " + groupedTask);
                        tasksToRetry.add(groupedTask);
                    } else {
                        final String serializedResult = rawResponse.getSerializedChroExtractionResult();
                        final ChroExtractionTaskResult result = MessagingSerialization.deserialize(serializedResult, ChroExtractionTaskResult.class);
                        if(result.getErrors().isEmpty()) {
                            throw new IllegalStateException("Invalid result without errors. Got ChroExtractionTaskResult with no errors," +
                                    " while expected CompositeRawChroExtractionResult. Task = " + groupedTask);
                        }
                        LOGGER.warn("Scheduled to retry the task which has been processed with errors: " + Arrays.toString(result.getErrors().toArray()) + ". Task = " + groupedTask);
                        tasksToRetry.add(groupedTask);
                    }
                    return null;
                }
            });
        }

        return new ExecutionContextHolder(rawExtractionJobs, tasksToRetry);
    }

    private void executeAndWire(ExecutorService executorService, List<Callable<CompositeRawChroExtractionResult>> rawExtractionJobs) {
        try {
            final List<Future<CompositeRawChroExtractionResult>> futures = executorService.invokeAll(rawExtractionJobs);
            final List<CompositeRawChroExtractionResult> results = newArrayList(Lists.transform(futures, future -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    throw new RuntimeException("Error sending the task", e);
                }
            }));

            for (final CompositeRawChroExtractionResult result : results) {
                if(result != null) {
                    final Map<ChroExtractionTaskId, String> serializedResult = result.getTaskIdToSerializedResult();
                    for (ChroExtractionTaskId taskId : serializedResult.keySet()) {
                        if (originalTasks.containsKey(taskId)) {
                            originalTasks.get(taskId).result = serializedResult.get(taskId);
                        } else {
                            throw new RuntimeException("De-grouping error. Cannot find the original task for the task ID = " + taskId);
                        }
                    }
                }
            }

        } catch (InterruptedException e) {
            throw new RuntimeException("Actual task sending interrupted.", e);
        }
    }

    private List<CompositeChroExtractionTask> groupTasksPerChunk(ExecutorService executorService, List<ChroExtractionTask> originalTaskList) {
        final List<CompositeChroExtractionTask> groupedTasks = new LinkedList<>();

        final Map<ChromatogramSource, List<ChroExtractionTask>> mapPerSource = new HashMap<>();
        for (ChroExtractionTask task : originalTaskList) {
            final ChromatogramSource chromatogramSource = task.getSources().get(0);
            if (!mapPerSource.containsKey(chromatogramSource)) {
                mapPerSource.put(chromatogramSource, new LinkedList<>());
            }
            mapPerSource.get(chromatogramSource).add(task);
        }

        final List<Callable<List<CompositeChroExtractionTask>>> jobs = new ArrayList<>();

        final RawDataAccessService rawDataAccessService = getRawDataAccessService();
        for (ChromatogramSource chromatogramSource : mapPerSource.keySet()) {
            jobs.add(() -> groupTasksForSource(mapPerSource, rawDataAccessService, chromatogramSource));
        }
        try {
            final List<Future<List<CompositeChroExtractionTask>>> futures = executorService.invokeAll(jobs);
            for (Future<List<CompositeChroExtractionTask>> future : futures) {
                final List<CompositeChroExtractionTask> groupedTasksForSource = future.get();
                groupedTasks.addAll(groupedTasksForSource);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error grouping tasks", e);
        }


        return groupedTasks;
    }

    private List<CompositeChroExtractionTask> groupTasksForSource(Map<ChromatogramSource, List<ChroExtractionTask>> mapPerSource, RawDataAccessService rawDataAccessService, ChromatogramSource chromatogramSource) throws IOException {
        final long startGrouping = System.currentTimeMillis();
        final List<CompositeChroExtractionTask> groupedTasksForSource = new LinkedList<>();
        final List<ChroExtractionTask> tasks = mapPerSource.get(chromatogramSource);
        final int rtShiftInt = (int) (chromatogramSource.getRtShift() * MzConversion.INT);


        final MSGrid msGrid = rawDataAccessService.obtainGridForRawFolder(chromatogramSource.getReference());

        final Map<Integer, List<ChroExtractionTask>> chunkIndexToTasks = new HashMap<>();

        for (ChroExtractionTask task : tasks) {
            //filter out non-chro tasks as well
            if (!task.getSpectrumToRender().equals(SpectrumType.TIC_CHROMATOGRAM)) {
                addSingleTask(groupedTasksForSource, task);
            } else {

                final MSRect campedMsRect = msGrid.campMSRect(task.getRawDataRange());
                final MSIndexRect indexRect = msGrid.getMSIndexRect(campedMsRect);

                //whole ranges can be calculated easily, so filter them out
                if (!(campedMsRect.isWholeRange() || campedMsRect.equals(msGrid.campMSRect(new MSRect())))) {
                    //full mz ranges can be calculated easily from existing spectrums, so filter them out.
                    if (msGrid.isFullMzRange(indexRect) && rtShiftInt == 0) {
                        addSingleTask(groupedTasksForSource, task);
                    } else {
                        final ChunkRange chunkRange = MS1DataService.calculateChunkRange(msGrid.getMetaData(), indexRect.startMz, indexRect.endMz);
                        //avoid grouping tasks which involve several chunks
                        final int startChunk = chunkRange.getStartChunk();
                        if (startChunk != chunkRange.getEndChunk()) {
                            addSingleTask(groupedTasksForSource, task);
                        } else {
                            //group these!
                            if (!chunkIndexToTasks.containsKey(startChunk)) {
                                chunkIndexToTasks.put(startChunk, new LinkedList<>());
                            }
                            chunkIndexToTasks.get(startChunk).add(task);
                        }
                    }

                } else {
                    addSingleTask(groupedTasksForSource, task);
                }
            }
        }

        for (Integer chunkIndex : chunkIndexToTasks.keySet()) {
            final CompositeChroExtractionTask composite = new CompositeChroExtractionTask();
            composite.setChunkIndex(chunkIndex);
            final List<ChroExtractionTask> tasksForCurrentChunk = chunkIndexToTasks.get(chunkIndex);
            composite.getTasks().addAll(tasksForCurrentChunk);
            if (tasksForCurrentChunk.size() <= 1) {
                composite.setChunkIndex(null);
            }
            groupedTasksForSource.add(composite);
        }
        final long elapsedTime = System.currentTimeMillis() - startGrouping;
        LOGGER.info("Created " + groupedTasksForSource.size() + " group(s) out of " + tasks.size() + " tasks for chro source " + chromatogramSource + " for " + elapsedTime + "ms.");
        return groupedTasksForSource;
    }

    private static void addSingleTask(List<CompositeChroExtractionTask> groupedTasks, ChroExtractionTask task) {
        final CompositeChroExtractionTask compositeTask = new CompositeChroExtractionTask();
        compositeTask.getTasks().add(task);
        groupedTasks.add(compositeTask);
    }

    private static final class ExtractionTaskAndResult {
        private final ChroExtractionTask task;
        private String result;

        private ExtractionTaskAndResult(ChroExtractionTask task) {
            this.task = task;
        }
    }

    private RawDataAccessService getRawDataAccessService() {
        return new PerFileRawDataAccessService(sharedStoragePath);
    }

    private static final class ExecutionContextHolder {
        private final Set<CompositeChroExtractionTask> tasksToRetry;
        private final List<Callable<CompositeRawChroExtractionResult>> jobs;

        private ExecutionContextHolder(List<Callable<CompositeRawChroExtractionResult>> jobs, Set<CompositeChroExtractionTask> tasksToRetry) {
            this.tasksToRetry = tasksToRetry;
            this.jobs = jobs;
        }

        public Set<CompositeChroExtractionTask> getTasksToRetry() {
            return tasksToRetry;
        }

        public List<Callable<CompositeRawChroExtractionResult>> getJobs() {
            return jobs;
        }
    }
}
