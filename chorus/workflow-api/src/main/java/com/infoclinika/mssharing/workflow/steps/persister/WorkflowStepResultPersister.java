package com.infoclinika.mssharing.workflow.steps.persister;

import com.infoclinika.mssharing.workflow.WorkflowRun;
import com.infoclinika.tasks.api.workflow.input.ProteinSearchTask;
import com.infoclinika.tasks.api.workflow.output.ProteinSearchTaskResult;

/**
 * Persist result of WorkflowStep execution and takes care of all cleanup for WorkflowStep.
 * This is one of the 3 major parts of WorkflowStep execution: preparing tasks, execute them, persist them.
 *
 * @author andrii.loboda
 */

public interface WorkflowStepResultPersister<TASK extends ProteinSearchTask, TASK_RESULT extends ProteinSearchTaskResult> {

    /**
     * Removes everything that have been persisted for current workflow step specified in workflowRun
     * according to setting passed as a parameter.
     *
     * @param workflowRun WorkflowRun with specified current step for which persisted results should be removed
     * @param settings    specifies how persisted results should be cleaned up
     */
    void removeResultsPersistedBefore(WorkflowRun workflowRun, RemoveResultsSettings settings);

    /**
     * Persists result in appropriate structure(database and cloud) provided in parameters
     *
     * @param result TaskResult as an object which represent WorkflowStep execution result
     */
    void persist(TASK_RESULT result);

}
