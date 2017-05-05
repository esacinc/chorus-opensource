package com.infoclinika.mssharing.workflow.steps;

import com.infoclinika.mssharing.workflow.WorkflowRun;
import com.infoclinika.tasks.api.workflow.WorkflowStepInput;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * @author andrii.loboda
 */
@Transactional
public interface WorkflowStepProgressMonitor<INPUT extends WorkflowStepInput> {
    void stepIsCompleted(WorkflowRun workflowRun, long totalTime);

    void oneTaskWasPersistedWithResults(WorkflowRun workflowRun, long persistDurationPerTask, long taskID);

    void oneTaskWasExecuted(WorkflowRun workflowRun, long pureTaskDuration, long totalExternalExecutionDuration, long taskID);

    void allTasksWerePreparedToExecute(WorkflowRun workflowRun, List<INPUT> preparedTasks, long prepareTasksDuration);

    long getStepTasksPreparationDuration(WorkflowRun workflowRun);

    void stepStarted(WorkflowRun workflowRun);

    long getStepStartedTimeStamp(WorkflowRun workflowRun);

    boolean isAllTasksCompleted(WorkflowRun workflowRun);

    void tasksWereRestarted(WorkflowRun workflowRun, Set<Long> taskIDs);
}
