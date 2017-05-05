package com.infoclinika.mssharing.model.write;

import java.util.List;

/**
 * @author Vladislav Kovchug
 */
public interface WorkflowManagement {
    void setWorkflowStepDisabled(long actor, long workflowStep, boolean disabled);

    void setWorkflowStepTypesToSkip(long actor, long workflowStep, List<Long> typesToSkip);

    void setWorkflowTemplateProperties(long actor, long workflowTemplate, boolean generateOneDatacube, boolean supportsLabeledExperiment);
}
