package com.infoclinika.mssharing.workflow.steps;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.infoclinika.mssharing.workflow.WorkflowRun;
import com.infoclinika.mssharing.workflow.WorkflowRun.WorkflowStepItem;
import com.infoclinika.mssharing.workflow.WorkflowRunner;
import com.infoclinika.mssharing.workflow.steps.persister.WorkflowStepResultPersister;
import com.infoclinika.mssharing.workflow.steps.preparator.WorkflowStepTaskPreparator;
import com.infoclinika.tasks.api.AbstractTask;
import com.infoclinika.tasks.api.workflow.input.ProteinSearchTask;
import com.infoclinika.tasks.api.workflow.output.ProteinSearchTaskResult;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Set;

/**
 * The class represents how each workflow step will be processed in workflow.
 * The main functionality of how workflow work is specified in this class
 * <p>
 * There are two possible scenarios how workflow step will be executed:
 * <p>
 * - asynchronously - each workflow steps send tasks and the current thread stops working. Instead, the other one listen for tasks results
 * Please NOTE, that only one thread per workflow step listens for task results, thus it reduces number of threads application uses.
 * To enable this mode, specify replyQueue to {@link WorkflowStepConfiguration#taskQueueConfiguration#replyQueue}
 * <p>
 * - synchronously - all operations are executed in one thread, i.e. application waits for the external execution results in this thread.
 * This scenario should be avoided for further workflow steps, because the application could be winded up with a lot of threads -  one per tasks
 *
 * @author andrii.loboda
 */
public abstract class AbstractWorkflowStepProcessor<INPUT extends ProteinSearchTask, OUTPUT extends ProteinSearchTaskResult> {
    private static final Logger LOG = LogManager.getLogger(AbstractWorkflowStepProcessor.class);

    /**
     * Prepares tasks for the step to be processed and sends them to external execution
     *
     * @param workflowRun Workflow Run with specified current step for which tasks should be created and sent
     */
    public final void prepareTasksAndSend(WorkflowRun workflowRun) {
        try {
            getWorkflowStepProgressMonitor().stepStarted(workflowRun);
            beforeWorkflowStepTasksCreation(workflowRun);
            final long startTimestamp = System.currentTimeMillis();
            LOG.info("Got task to process in workflow step: " + workflowRun.currentStep.name);
            validate(workflowRun);
            final List<INPUT> preparedTasksToSend = getPreparator().prepare(workflowRun);
            validate(workflowRun);
            final long prepareTasksDuration = System.currentTimeMillis() - startTimestamp;
            LOG.info("Prepared tasks to send for step: " + workflowRun.currentStep.name + " in " + ((prepareTasksDuration) / 60000) + " minutes, tasks  count: " + preparedTasksToSend.size());
            getWorkflowStepProgressMonitor().allTasksWerePreparedToExecute(workflowRun, preparedTasksToSend, prepareTasksDuration);

            afterAllTasksPrepared(workflowRun, preparedTasksToSend); // remove old persisted content for this workflow run for this workflow step
            sendTasks(workflowRun, preparedTasksToSend);
        } catch (Exception e) {
            LOG.error("Workflow has thrown an exception during preparing tasks and sending them.");
            getWorkflowRunner().getErrorHandler().onError(workflowRun, e);
        }

    }

    private void sendTasks(WorkflowRun workflowRun, List<INPUT> preparedTasksToSend) {
        for (INPUT preparedTask : preparedTasksToSend) {
            afterInputIsGeneratedHandler(preparedTask);
        }

        final WorkflowStepConfiguration configuration = getConfiguration(workflowRun);
        LOG.info("Created configuration for executing workflow step: " + workflowRun.currentStep.name + ", configuration: " + configuration);
        LOG.info("Sending prepared tasks to queue: " + configuration + " for step" + workflowRun.currentStep.name);
        validate(workflowRun);
        if (configuration.taskQueueConfiguration.replyQueue != null) {
            sendTasksAsync(workflowRun, preparedTasksToSend, configuration);
        } else {
            sendTasksSync(workflowRun, preparedTasksToSend, configuration);
        }
    }

    /**
     * Prepares tasks for the following workflow run.
     */
    public final void prepareTasksAndSend(WorkflowRun workflowRun, Set<Long> taskIDs) {
        final Optional<List<INPUT>> preparedTasksToSend = tryToRestoreTasks(workflowRun, taskIDs);
        final List<INPUT> tasksToSend;
        if (preparedTasksToSend.isPresent()) {
            tasksToSend = preparedTasksToSend.get();
            sendTasks(workflowRun, tasksToSend);
            getWorkflowStepProgressMonitor().tasksWereRestarted(workflowRun, taskIDs); //remove tasks from executed and persisted
        } else {
            prepareTasksAndSend(workflowRun);
        }

    }

    /**
     * Handles each task result from external execution for the step: validates, persists, and moves to next workflow step in workflow
     *
     * @param workflowRun Workflow run with specified current step for which task result should be processed
     * @param taskResult  Task Result to process
     */
    public final void processTaskResultAndPersist(WorkflowRun workflowRun, OUTPUT taskResult) {
        try {
            final long taskID = ((AbstractTask) taskResult.input).getUniqueId();
            if (taskResultIsRelevant(workflowRun, taskResult)) {
                afterOutputIsObtainedHandler(taskResult);
                final long totalExternalExecutionDuration;
                final long stepStartedTimeStamp;
                validate(workflowRun);
                synchronized (this) {
                    final long prepareTasksDuration = getWorkflowStepProgressMonitor().getStepTasksPreparationDuration(workflowRun);
                    stepStartedTimeStamp = getWorkflowStepProgressMonitor().getStepStartedTimeStamp(workflowRun);
                    totalExternalExecutionDuration = System.currentTimeMillis() - stepStartedTimeStamp - prepareTasksDuration;
                    getWorkflowStepProgressMonitor().oneTaskWasExecuted(workflowRun, taskResult.duration, totalExternalExecutionDuration, taskID);
                }
                LOG.debug("Received " + taskResult.getClass() + " +  result for step " + workflowRun.currentStep + "  worker for protein search ID " + workflowRun.id + ": " + taskResult);

                if (!Strings.isNullOrEmpty(taskResult.errorMessage)) {
                    final String message = "Task Result returned with error message " + taskResult.errorMessage + ", output: " + taskResult;
                    LOG.error(message);
                    throw new RuntimeException(taskResult.errorMessage);
                }

                synchronized (this) {
                    final long persistStartTimeStamp = System.currentTimeMillis();
                    validate(workflowRun);
                    getPersister().persist(taskResult);
                    validate(workflowRun);

                    final long persistDurationPerTask = System.currentTimeMillis() - persistStartTimeStamp;
                    getWorkflowStepProgressMonitor().oneTaskWasPersistedWithResults(workflowRun, persistDurationPerTask, taskID);
                }

                if (allDataPersisted(workflowRun)) {
                    final long totalExternalExecutionDurationInMin = totalExternalExecutionDuration / 60000;
                    LOG.info("Completed external execution of workflow step: " + workflowRun.currentStep.name + " in " + totalExternalExecutionDurationInMin + " min.  task result:" + taskResult);

                    final long totalTime = System.currentTimeMillis() - stepStartedTimeStamp;
                    getWorkflowStepProgressMonitor().stepIsCompleted(workflowRun, totalTime);
                    LOG.info("Total time for workflow step:" + workflowRun.currentStep.name + ((totalTime) / 60000) + " minutes. ");
                    if (workflowRun.lastStep) {
                        getWorkflowRunner().getWorkflowCompletedHandler().onCompleted(workflowRun);
                    } else {
                        getWorkflowRunner().moveToNextStep(workflowRun, taskResult);
                    }

                }
            } else {
                LOG.warn("Caught a task result but it is not actual for WorkflowRun, workflow ID: " + workflowRun.id + ", task result:" + taskResult);
                LOG.info("Caught wrong task ID is: " + taskID);
            }

        } catch (RuntimeException e) {
            LOG.error("Workflow has thrown an exception during persisting task result.");
            getWorkflowRunner().getErrorHandler().onError(workflowRun, e);
        }

    }


    //    abstract methods for preparing tasks and sending to queue
    public abstract WorkflowStepTaskPreparator<INPUT> getPreparator();

    /**
     * Prepares workflow step for normal work: setup reply queue listener
     */
    public abstract void initialize(WorkflowStepItem workflowStep);

    protected abstract void afterInputIsGeneratedHandler(INPUT preparedTask);

    /**
     * Tries to restore tasks to add them as job for external processing.
     *
     * @param workflowRun Workflow run with current workflow step
     * @param taskIDs tasks to restore
     * @return {@link Optional#absent()} if restoring was not successful or restored tasks
     */
    protected abstract Optional<List<INPUT>> tryToRestoreTasks(WorkflowRun workflowRun, Set<Long> taskIDs);


    protected abstract WorkflowStepConfiguration getConfiguration(WorkflowRun workflowRun);

    protected abstract void sendTasksAsync(WorkflowRun workflowRun, List<INPUT> preparedTasksToSend, WorkflowStepConfiguration configuration);

    protected abstract void sendTasksSync(WorkflowRun workflowRun, List<INPUT> preparedTasksToSend, WorkflowStepConfiguration configuration);

//    abstract methods to persist task result and move to the next step

    protected abstract void afterOutputIsObtainedHandler(OUTPUT taskResult);

    public abstract WorkflowStepResultPersister<INPUT, OUTPUT> getPersister();

    protected abstract boolean allDataPersisted(WorkflowRun workflowRun);

    //    Miscellaneous
    protected abstract WorkflowRunner getWorkflowRunner();

    protected abstract void validate(WorkflowRun workflowRun);

    public abstract void stopStep(WorkflowRun workflowRun);

    protected abstract WorkflowStepProgressMonitor getWorkflowStepProgressMonitor();


    protected void afterAllTasksPrepared(WorkflowRun workflowRun, List<INPUT> preparedTasks) {

    }

    protected void beforeWorkflowStepTasksCreation(WorkflowRun workflowRun){}

    protected boolean taskResultIsRelevant(WorkflowRun workflowRun, OUTPUT taskResult) {
        return true;
    }

}




