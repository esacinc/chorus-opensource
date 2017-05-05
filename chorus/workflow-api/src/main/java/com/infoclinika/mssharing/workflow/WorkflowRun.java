package com.infoclinika.mssharing.workflow;

import com.google.common.base.MoreObjects;
import com.infoclinika.mssharing.workflow.steps.AbstractWorkflowStepProcessor;
import com.infoclinika.tasks.api.workflow.output.ProteinSearchTaskResult;

/**
 * Class represent ubiquitous transfer object for Workflow execution classes.
 * This class have all necessary fields for application to identify it.
 *
 * @author andrii.loboda
 */
public class WorkflowRun {
    public final long id;
    public final WorkflowStepItem currentStep;
    public final boolean lastStep;
    public final ProteinSearchTaskResult previousResult;

    /**
     * @param id             Workflow Run identifier. Should be unique.
     * @param currentStep    Current step which is executing right now in Workflow for this Workflow Run
     * @param lastStep       Specifies whether the current step is the last one in Workflow
     * @param previousResult Result of previous workflow step
     */
    public WorkflowRun(long id, WorkflowStepItem currentStep, boolean lastStep, ProteinSearchTaskResult previousResult) {
        this.id = id;
        this.currentStep = currentStep;
        this.lastStep = lastStep;
        this.previousResult = previousResult;
    }

    /**
     * Current step which is executing right now in Workflow for this Workflow Run
     */
    public static final class WorkflowStepItem {
        public final long id;
        public final long typeID;
        public final String name;
        public final Class<? extends AbstractWorkflowStepProcessor> processorClass;
        public final String uploadKeyPrefix;
        public final boolean runOnlyNotComplete;

        /**
         * @param id                 Workflow step ID
         * @param typeID             Workflow step type identifier. Should be unique in workflow
         * @param name               Workflow step name
         * @param processorClass     Class who is in charge to process the workflow step
         * @param uploadKeyPrefix    prefix for cloud references to upload results for the step
         * @param runOnlyNotComplete specified whether all tasks should be run(if several) or just those which weren't successful
         */
        public WorkflowStepItem(long id, long typeID, String name, Class<? extends AbstractWorkflowStepProcessor> processorClass,
                                String uploadKeyPrefix, boolean runOnlyNotComplete) {
            this.id = id;
            this.typeID = typeID;
            this.name = name;
            this.processorClass = processorClass;
            this.uploadKeyPrefix = uploadKeyPrefix;
            this.runOnlyNotComplete = runOnlyNotComplete;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", id)
                    .add("typeID", typeID)
                    .add("name", name)
                    .add("processorClass", processorClass)
                    .add("uploadKeyPrefix", uploadKeyPrefix)
                    .add("runOnlyNotComplete", runOnlyNotComplete)
                    .toString();
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("currentStep", currentStep)
                .add("lastStep", lastStep)
                .toString();
    }
}
