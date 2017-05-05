package com.infoclinika.mssharing.workflow;

/**
 * Represents error handler for workflow execution.
 * If workflow fails in any point, this {@link WorkflowErrorHandler#onError} will be called
 *
 * @author andrii.loboda
 */
public interface WorkflowErrorHandler {
    /**
     * Handles any exception occurred during Workflow Run execution.
     *
     * @param workflowRun Workflow Run which has thrown an exception
     * @param exception   Exception which has been thrown during workflow execution
     */
    void onError(WorkflowRun workflowRun, Exception exception);
}
