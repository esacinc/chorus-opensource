package com.infoclinika.mssharing.workflow;

import com.google.common.base.Optional;
import com.infoclinika.tasks.api.workflow.output.ProteinSearchTaskResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsible for running/stopping workflows and deals with workflow results
 *
 * @author andrii.loboda
 */

public interface WorkflowRunner {
    /**
     * Runs Workflow for workflow run, if this action is allowed for user.
     * Executed asynchronously.
     */
    void run(long actor, long processingRun);

    /**
     * Runs Workflow for workflow run from specific step, in case steps before have been completed successfully.
     * Checks whether this action is allowed for user who initiated it.
     * Executed asynchronously.
     *
     * @param actor                 user who initiated this action
     * @param processingRun         Workflow ID which should be re-run
     * @param startWithWorkflowStep step from which workflow should be re-run.
     *                              If {@link Optional#absent()}, workflow will be re-run after the last successful step
     */
    void restartFromStep(long actor, long processingRun, Optional<Long> startWithWorkflowStep, boolean runOnlyNotComplete);

    /**
     * Marks workflow run as cancelled, if this action is allowed for actor who initiated it
     */
    @Transactional
    void cancel(long actor, long processingRun);

    /**
     * Marks all processing runs which are in progress to be cancelled by application.
     * NOTE: Should be called only by system, users shouldn't trigger this method
     */
    @Transactional
    void cancelAllByApp();

    /**
     * Runs all processing runs which are cancelled by aoplication now(usually by production deployment).
     * NOTE: Should be called only by admin
     */
    @Transactional
    void restartAllCancelledByApp(long actor);

    /**
     * Checks whether actor can remove workflow step results of workflow and if he can,
     * removes all workflow step results.
     * Note, that the results of the total workflow work will NOT be removed, only temporary files.
     *
     * @param actor         user who initiates an action
     * @param processingRun workflow ID which results should be cleaned up
     */
    @Transactional
    void cleanResults(long actor, long processingRun);

    /**
     * Should not be invoked manually. Returns instance of class which is able
     * to handle appropriately any exceptions occurred in workflow execution
     */
    WorkflowErrorHandler getErrorHandler();

    /**
     * Should not be invoked manually. Returns instance of class which is able
     * to handle appropriately workflow which processed all requred steps to complete workflow
     */
    WorkflowCompletedHandler getWorkflowCompletedHandler();

    /**
     * * Should not be invoked manually. Invoked when workflow execution step is completed and workflow has more steps to execute
     * In this case, it changes current workflow step to the next one to execute(which goes after completed step).
     *
     * @param workflowRun    Current workflow run's state
     * @param previousResult result of previous workflow step in workflow
     */
    <OUTPUT extends ProteinSearchTaskResult> void moveToNextStep(final WorkflowRun workflowRun, OUTPUT previousResult);

    /**
     * Should not be invoked manually. Invoked by Workflow step to get current execution step and its workflow in case this data is not available.
     *
     * @param workflowRunID Workflow Run ID which is in progress
     * @return basic data about workflow execution.
     */
    WorkflowRun getCurrentStep(final long workflowRunID);

    /**
     * Should not be invoked manually. Invoked by application to setup all queue listeners, re-run lost tasks for workflow runs which are in progress now.
     */
    void initialize();
}
