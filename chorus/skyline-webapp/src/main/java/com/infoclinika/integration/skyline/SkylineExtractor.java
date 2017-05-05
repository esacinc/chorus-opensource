package com.infoclinika.integration.skyline;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.infoclinika.analysis.storage.cloud.CloudStorageItemReference;
import com.infoclinika.chorus.integration.skyline.api.ChromatogramExtractor;
import com.infoclinika.chorus.integration.skyline.api.SingleSpectrumExtractionRequest;
import com.infoclinika.common.utils.Pair;
import com.infoclinika.mssharing.model.extraction.exception.ChroExtractionException;
import computations.MessagingComputationsClient;
import computations.impl.ChroExtractionClient;
import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.GroupPoints;
import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.chromatogramrequest.ChromSource;
import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.chromatogramrequest.ChromatogramRequestDocument;
import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.chromatogramrequest.Ms2FullScanAcquisitionMethod;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

/**
 * @author Oleksii Tymchenko
 */
public class SkylineExtractor extends ChromatogramExtractor {
    private static final Logger LOGGER = Logger.getLogger(SkylineExtractor.class);

    public static final int DEFAULT_EXTRACTION_PARALLELISM = 20;
    public static final int DEFAULT_PROCESSING_PARALLELISM = 20;
    public static final int CE_EXTRACTION_PARALLELISM = 5;
    public static final boolean groupingEnabled = true;

    private final ExecutorService executorService;
    private final ExecutorService lazyProcessingService;
    private final ChroExtractionClient computationsClient;
    private final String sharedStoragePath;
    private final int extractionParallelism;
    private final int processingParallelism;

    public SkylineExtractor(ChroExtractionClient computationsClient, String sharedStoragePath, int extractionParallelism, int processingParallelism) {
        this.computationsClient = computationsClient;
        this.sharedStoragePath = sharedStoragePath;
        this.extractionParallelism = extractionParallelism;
        this.processingParallelism = processingParallelism;
        executorService = Executors.newFixedThreadPool(this.extractionParallelism);
        lazyProcessingService = Executors.newFixedThreadPool(this.processingParallelism);
    }

    @Override
    protected Iterable<GroupPoints> extract(Set<String> ms1FilterRefs, Set<String> ms2FilterRefs,
                                            Set<String> simFilterRefs, ChromatogramRequestDocument request) {
        final Set<String> filteredMs1 = ExtractionContentExpert.removeUnwantedFilters(ms1FilterRefs);
        final Set<String> filteredMs2 = ExtractionContentExpert.removeUnwantedFilters(ms2FilterRefs);
        final Set<String> filteredSim = ExtractionContentExpert.removeUnwantedFilters(simFilterRefs);
        return doExtraction(filteredMs1, filteredMs2, filteredSim, request, LAZY_EXTRACTION_RESULTS_FN);
    }

    @Override
    protected void extractAndWrite(Set<String> ms1FilterRefs, Set<String> ms2FilterRefs,
                                            Set<String> simFilterRefs, SingleSpectrumExtractionRequest request,
                                            OutputStream destination) {
        final Set<String> filteredMs1 = ExtractionContentExpert.removeUnwantedFilters(ms1FilterRefs);
        final Set<String> filteredMs2 = ExtractionContentExpert.removeUnwantedFilters(ms2FilterRefs);
        final Set<String> filteredSim = ExtractionContentExpert.removeUnwantedFilters(simFilterRefs);

        doSingleExtraction(filteredMs1, filteredMs2, filteredSim, request, destination, LAZY_SINGLE_EXTRACTION_FN);
    }

    private Iterable<GroupPoints> doExtraction(Set<String> ms1FilterRefs,
                                                   Set<String> ms2FilterRefs,
                                                   Set<String> simFilterRefs,
                                                   ChromatogramRequestDocument request,
                                               ExtractionResultsFunction function) {
        LOGGER.debug("Extracting chromatograms from file." +
                        " MS1: " + setToString(ms1FilterRefs) +
                        ". MS2: " + setToString(ms2FilterRefs) +
                        ". SIM: " + setToString(simFilterRefs)
        );

        if (request.getMs2FullScanAcquisitionMethod().equals(Ms2FullScanAcquisitionMethod.DIA) && ms2FilterRefs == null) {
            throw new ChroExtractionValidationException("MS2 filter content is required for DIA data");
        }

        final Set<CloudStorageItemReference> ms1Contents =  ExtractionContentExpert.parseMultipleRefs(ms1FilterRefs);
        final Set<CloudStorageItemReference> ms2Contents = ExtractionContentExpert.parseMultipleRefs(ms2FilterRefs);
        final Set<CloudStorageItemReference> simContents = ExtractionContentExpert.parseMultipleRefs(simFilterRefs);
        final List<ChromatogramRequestDocument.ChromatogramGroup> chroGroups = request.getChromatogramGroup();

        final List<ExtractionResult> jobResults = function.obtain(request, ms1Contents, ms2Contents, simContents, chroGroups);

        final List<GroupPoints> groupPoints = Lists.transform(jobResults, new Function<ExtractionResult, GroupPoints>() {
            @Override
            public GroupPoints apply(ExtractionResult extractionResult) {
                return extractionResult.getGroupPoints();
            }
        });

        return new ArrayList<>(groupPoints);
    }


    private void doSingleExtraction(Set<String> ms1FilterRefs, Set<String> ms2FilterRefs, Set<String> simFilterRefs,
                                    final SingleSpectrumExtractionRequest request,
                                    OutputStream destination,
                                    final SingleExtractionResultFunction function) {

        final Set<CloudStorageItemReference> ms1Contents =  ExtractionContentExpert.parseMultipleRefs(ms1FilterRefs);
        final Set<CloudStorageItemReference> ms2Contents = ExtractionContentExpert.parseMultipleRefs(ms2FilterRefs);
        final Set<CloudStorageItemReference> simContents = ExtractionContentExpert.parseMultipleRefs(simFilterRefs);

        final int driftFnCount = ExtractionContentExpert.getDriftFnCount(ms1Contents, ms2Contents);
        final boolean isMs2Ce = ExtractionContentExpert.detectCollisionEnergyData(ms2Contents);
        final boolean isMs1Ce = ExtractionContentExpert.detectCollisionEnergyData(ms1Contents);
        if(request.driftTime == null) {
            if(isMs2Ce) {
                //pick first CE function as the only MS2
                final Set<CloudStorageItemReference> filtered = ExtractionContentExpert.pickFirstFn(ms2Contents);
                ms2Contents.clear();
                ms2Contents.addAll(filtered);
            }
            if(isMs1Ce) {
                final Set<CloudStorageItemReference> filtered = ExtractionContentExpert.pickFirstFn(ms1Contents);
                ms1Contents.clear();
                ms1Contents.addAll(filtered);
            }
            extractSingleResult(request, destination, function, ms1Contents, ms2Contents, simContents, driftFnCount);
        } else if (request.driftTime < 0) {
            extractArrayOfResults(request, destination, function, ms1Contents, ms2Contents, driftFnCount, isMs2Ce);
        } else {
            extractSingleResultForSpecificTime(request, destination, function, ms1Contents, ms2Contents);
        }
    }

    private static void extractSingleResultForSpecificTime(SingleSpectrumExtractionRequest request, OutputStream destination,
                                                           SingleExtractionResultFunction function,
                                                           Set<CloudStorageItemReference> ms1Contents,
                                                           Set<CloudStorageItemReference> ms2Contents) {
        final SortedMap<Double, Pair<CloudStorageItemReference, CloudStorageItemReference>> driftTimeToFns =
                ExtractionContentExpert.layoutByDriftTime(ms1Contents, ms2Contents);
        Pair<CloudStorageItemReference, CloudStorageItemReference> contents = ExtractionContentExpert.findExactPair(driftTimeToFns, request.driftTime);
        if(contents == null) {
            throw new IllegalArgumentException("Cannot find exact drift time for time: " + request.driftTime);
        }
        final ExtractionResult extractionResult = function.obtain(request,
                newHashSet(contents.getFirst()),
                newHashSet(contents.getSecond()),
                Sets.<CloudStorageItemReference>newHashSet());
        final String response = toString(new DriftTime(request.driftTime), extractionResult) ;

        writeResponseQuietly(destination, response);
    }

    private static void extractArrayOfResults(final SingleSpectrumExtractionRequest request, OutputStream destination,
                                              final SingleExtractionResultFunction function,
                                              Set<CloudStorageItemReference> ms1Contents,
                                              Set<CloudStorageItemReference> ms2Contents,
                                              int driftFnCount, boolean isMs2Ce) {
        //new endpoint; return array of results
        if(driftFnCount > 0) {
            //per drift time;
            LOGGER.info("[IMS Data] Extracting single spectra for " + request + " in " + driftFnCount + " parallel streams.");
            final ExecutorService perDriftTimeExecutor = Executors.newFixedThreadPool(driftFnCount);
            final List<Callable<ExtractionResult>> jobs = new ArrayList<>(driftFnCount);
            final SortedMap<Double, Pair<CloudStorageItemReference, CloudStorageItemReference>> driftTimeToFns = ExtractionContentExpert.layoutByDriftTime(ms1Contents, ms2Contents);
            final Set<Double> allDriftTimes = driftTimeToFns.keySet();
            for (final Double driftTime : allDriftTimes) {
                jobs.add(new Callable<ExtractionResult>() {
                    @Override
                    public ExtractionResult call() throws Exception {
                        final Pair<CloudStorageItemReference, CloudStorageItemReference> contents = driftTimeToFns.get(driftTime);
                        return function.obtain(request,
                                newHashSet(contents.getFirst()),
                                newHashSet(contents.getSecond()),
                                Collections.<CloudStorageItemReference>emptySet()
                        );
                    }
                });
            }
            final List<ExtractionResult> resultsPerDriftTime = obtainResultsInParallel(perDriftTimeExecutor, jobs, "Error extracting single spectra results for RT and all drift times");

            final List<Double> allDriftTimesList = newArrayList(allDriftTimes);
            final List<String> stringifiedResults = newArrayList();
            for (int i = 0; i < allDriftTimesList.size(); i++) {
                Double driftTime = allDriftTimesList.get(i);
                final ExtractionResult extractionResult = resultsPerDriftTime.get(i);
                final String currentResult = toString(new DriftTime(driftTime), extractionResult);
                stringifiedResults.add(currentResult);
            }
            final String response = "{ \"results\": [" + Joiner.on(", ").join(stringifiedResults) + "]}";
            writeResponseQuietly(destination, response);
        } else if(isMs2Ce) {
            //per collision energy for MS2; or just a single result in an array for MS1.
            final String response;
            if(request.chromSource.equals(ChromSource.MS_1)) {
                final ExtractionResult result = function.obtain(request,
                        ms1Contents,
                        Collections.<CloudStorageItemReference>emptySet(),
                        Collections.<CloudStorageItemReference>emptySet()
                );
                response = "{ \"results\": [" + toString(result) + "]}";
            } else {
                final ExecutorService ms2CeExecutor = Executors.newFixedThreadPool(CE_EXTRACTION_PARALLELISM);
                final List<Callable<ExtractionResult>> jobs = new ArrayList<>(ms2Contents.size());
                final List<Double> ceValues = new ArrayList<>(ms2Contents.size());
                for (final CloudStorageItemReference ms2Content : ms2Contents) {
                    ceValues.add(ExtractionContentExpert.parseCEValue(ms2Content));
                    jobs.add(new Callable<ExtractionResult>() {
                        @Override
                        public ExtractionResult call() throws Exception {
                            return function.obtain(request,
                                    Collections.<CloudStorageItemReference>emptySet(),
                                    newHashSet(ms2Content),
                                    Collections.<CloudStorageItemReference>emptySet());
                        }
                    });
                }

                final List<ExtractionResult> extractionResults = obtainResultsInParallel(ms2CeExecutor, jobs, "Error extracting multiple results for all-ions MS2");
                final List<String> stringifiedResults = new ArrayList<>(extractionResults.size());
                //order is the same for jobs, extractionResults and ceValues;
                for (int i = 0; i < extractionResults.size(); i++) {
                    final ExtractionResult extractionResult = extractionResults.get(i);
                    final Double ceValue = ceValues.get(i);
                    final String stringifiedCeResult = toString(new CollisionEnergy(ceValue), extractionResult);
                    stringifiedResults.add(stringifiedCeResult);
                }
                response = "{ \"results\": [" + Joiner.on(", ").join(stringifiedResults) + "]}";
            }
            writeResponseQuietly(destination, response);
        } else {
            //single result in an array
            final ExtractionResult result = function.obtain(request, ms1Contents, ms2Contents, Collections.<CloudStorageItemReference>emptySet());
            final String response = "{ \"results\": [" + toString(result) + "]}";
            writeResponseQuietly(destination, response);
        }
    }

    private static List<ExtractionResult> obtainResultsInParallel(ExecutorService executor, List<Callable<ExtractionResult>> jobs, String errorMessage) {
        final List<Future<ExtractionResult>> futures;
        try {
            futures = executor.invokeAll(jobs);
        } catch (InterruptedException e) {
            throw new RuntimeException(errorMessage, e);
        }
        final List<ExtractionResult> resultsPerDriftTime = Lists.transform(futures, new Function<Future<ExtractionResult>, ExtractionResult>() {
            @Nullable
            @Override
            public ExtractionResult apply(Future<ExtractionResult> extractionResultFuture) {
                try {
                    return extractionResultFuture.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        executor.shutdownNow();
        return resultsPerDriftTime;
    }

    private static void writeResponseQuietly(OutputStream destination, String response) {
        try {
            IOUtils.write(response, destination);
        } catch (IOException e) {
            throw new RuntimeException("Error writing the response to the output stream", e);
        }
    }

    private static void extractSingleResult(SingleSpectrumExtractionRequest request, OutputStream destination, SingleExtractionResultFunction function, Set<CloudStorageItemReference> ms1Contents, Set<CloudStorageItemReference> ms2Contents, Set<CloudStorageItemReference> simContents, int driftFnCount) {
        //plain old endpoint; return single result for all fns
        if (driftFnCount > 0) {
            final CloudStorageItemReference ms1GlobalFn = ExtractionContentExpert.findPrecalculatedContent(ms1Contents, ExtractionContentExpert.GLOBAL_FN_PREFIX_MS1);
            final CloudStorageItemReference ms2GlobalFn = ExtractionContentExpert.findPrecalculatedContent(ms2Contents, ExtractionContentExpert.GLOBAL_FN_PREFIX_MS2);
            ms1Contents.clear();
            ms1Contents.add(ms1GlobalFn);
            ms2Contents.clear();
            ms2Contents.add(ms2GlobalFn);
        }

        final ExtractionResult extractionResult = function.obtain(request, ms1Contents, ms2Contents, simContents);
        final String response = "{" + toString(extractionResult) + "}";

        writeResponseQuietly(destination, response);
    }

    private static String toString(DriftTime driftTime, ExtractionResult extractionResult) {
        return "{" + toString(extractionResult) + ", \"driftTime\": " + driftTime.value + "}";
    }

    private static String toString(CollisionEnergy collisionEnergy, ExtractionResult extractionResult) {
        return "{" + toString(extractionResult) + ", \"collisionEnergy\": " + collisionEnergy.value + "}";
    }

    private static String toString(ExtractionResult extractionResult) {
        final SingleSpectrumExtractionResult result = extractionResult.getSpectrumExtractionResult();
        String mzsBase64 = toBase64String(result.mzs);
        String intensitiesBase64 = toBase64String(result.intensitites);

        return "\"index\": " + result.index
        + ", \"rt\": " + result.rt
        + ", \"mzs-base64\": \"" + mzsBase64
        + "\" , \"intensities-base64\": \"" + intensitiesBase64 + "\"";
    }


    private interface ExtractionResultsFunction {
        List<ExtractionResult> obtain(ChromatogramRequestDocument request,
                                      Set<CloudStorageItemReference> ms1Contents,
                                      Set<CloudStorageItemReference> ms2Contents,
                                      Set<CloudStorageItemReference> simContents,
                                      List<ChromatogramRequestDocument.ChromatogramGroup> chroGroups);
    }

    private interface SingleExtractionResultFunction {
        ExtractionResult obtain(SingleSpectrumExtractionRequest request,
                                Set<CloudStorageItemReference> ms1Contents,
                                Set<CloudStorageItemReference> ms2Contents,
                                Set<CloudStorageItemReference> simContents);
    }


    private  final ExtractionResultsFunction LAZY_EXTRACTION_RESULTS_FN = new ExtractionResultsFunction() {

        @Override
        public List<ExtractionResult> obtain(ChromatogramRequestDocument request,
                                             Set<CloudStorageItemReference> ms1Contents,
                                             Set<CloudStorageItemReference> ms2Contents,
                                             Set<CloudStorageItemReference> simContents,
                                             List<ChromatogramRequestDocument.ChromatogramGroup> chroGroups) {
            final MessagingComputationsClient groupingExtractionClient;
            if(groupingEnabled) {
                groupingExtractionClient = new SkylineGroupingExtractionClient(computationsClient, sharedStoragePath);
            } else {
                groupingExtractionClient = computationsClient;
            }

            final List<LazyRequestResultExtractionJob> jobs = new ArrayList<>(chroGroups.size());
            for (int i = 0; i < chroGroups.size(); i++) {
                final ChromatogramRequestDocument.ChromatogramGroup chroGroup = chroGroups.get(i);
                jobs.add(new LazyRequestResultExtractionJob(i, groupingExtractionClient, chroGroup, ms1Contents, ms2Contents, simContents, request));
            }

            final List<ExtractionResult> jobResults = new LinkedList<>();
            try {
                final long beforeJobsProcessing = System.currentTimeMillis();
                final List<Future<LazyExtractionResult>> futures = executorService.invokeAll(jobs);

                if(groupingExtractionClient instanceof SkylineGroupingExtractionClient) {
                    ((SkylineGroupingExtractionClient)groupingExtractionClient).groupAndSendAllWith(executorService);
                }

                LOGGER.info("  All RAW jobs (" + jobs.size() + ") have been processed for " + (System.currentTimeMillis() - beforeJobsProcessing)
                        + " ms in " + extractionParallelism + " threads");
                final long unpackStart = System.currentTimeMillis();

                final List<Callable<ExtractionResult>> postProcessingJobs = newArrayList(Lists.transform(futures,
                        new Function<Future<LazyExtractionResult>, Callable<ExtractionResult>>() {
                    @Override
                    public Callable<ExtractionResult> apply(Future<LazyExtractionResult> future) {
                        final LazyExtractionResult lazyResult;
                        try {
                            lazyResult = future.get();
                        } catch (Exception e) {
                            LOGGER.warn("Error UNPACKING extraction result; continuing anyway", e);
                            throw new RuntimeException(e);
                        }
                        return new Callable<ExtractionResult>() {
                            @Override
                            public ExtractionResult call() throws Exception {
                                return lazyResult.get();
                            }
                        };

                    }
                }));

                final List<Future<ExtractionResult>> finalFutures = lazyProcessingService.invokeAll(postProcessingJobs);

                for (Future<ExtractionResult> future : finalFutures) {
                    ExtractionResult extractionResult = null;
                    try {
                        extractionResult = future.get();
                    } catch (Exception exception) {
                        LOGGER.warn("Error obtaining FINAL extraction result; continuing anyway", exception);
                    }
                    if(extractionResult != null) {
                        jobResults.add(extractionResult);
                    }
                }
                LOGGER.info("  All RAW jobs (" + jobs.size() + ") have been UNPACKED for " + (System.currentTimeMillis() - unpackStart) + " ms in " + processingParallelism + " processing threads");
            } catch (Exception e) {
                throw new RuntimeException("Unexpected error processing extraction jobs", e);
            }
            Collections.sort(jobResults);
            return jobResults;
        }
    };


    private final SingleExtractionResultFunction LAZY_SINGLE_EXTRACTION_FN = new SingleExtractionResultFunction() {
        @Override
        public ExtractionResult obtain(SingleSpectrumExtractionRequest request,
                                       Set<CloudStorageItemReference> ms1Contents,
                                       Set<CloudStorageItemReference> ms2Contents,
                                       Set<CloudStorageItemReference> simContents) {


            final LazySingleSpectrumExtractionJob job = new LazySingleSpectrumExtractionJob(computationsClient,
                    ms1Contents, ms2Contents, simContents,
                    request);
            final ExtractionResult extractionResult;
            try {
                extractionResult = job.call().get();
            } catch (ChroExtractionException e) {
                LOGGER.error("Cannot fire single spectrum extraction call", e);
                throw e;
            } catch (Exception e) {
                LOGGER.error("Error extracting single spectra", e);
                throw new RuntimeException(e);
            }
            return extractionResult;
        }
    };


    //Utils. todo: consider moving to another class

    private static String toBase64String(float[] array) {
        byte[] bytes = new byte[array.length * 4];
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        for (float value : array) {
            buf.putFloat(value);
        }
        return new String(Base64.encodeBase64(bytes));
    }

    private static String toBase64String(double[] array) {
        byte[] bytes = new byte[array.length * 8];
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        for (double value : array) {
            buf.putDouble(value);
        }
        return new String(Base64.encodeBase64(bytes));
    }

    private static String setToString(Set<String> strings) {
        return strings == null ? "[]" : Arrays.toString(strings.toArray());
    }

    private static final class DriftTime {
        public final Double value;

        private DriftTime(Double value) {
            this.value = value;
        }
    }

    private static final class CollisionEnergy {
        public final Double value;

        private CollisionEnergy(Double value) {
            this.value = value;
        }
    }
}
