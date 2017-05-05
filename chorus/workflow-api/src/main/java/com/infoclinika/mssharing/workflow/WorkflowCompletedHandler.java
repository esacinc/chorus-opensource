package com.infoclinika.mssharing.workflow;

/**
 * Represents completion handler for workflow execution.
 * When WorkflowRun is completed {@link WorkflowCompletedHandler#onCompleted} will be called
 *
 * @author andrii.loboda
 */
public interface WorkflowCompletedHandler {

    /**
     * Workflow Run completion handler which is executed when workflow is passed successfully
     *
     * @param workflowRun WorkflowRun which is completed
     */
    void onCompleted(WorkflowRun workflowRun);
}
