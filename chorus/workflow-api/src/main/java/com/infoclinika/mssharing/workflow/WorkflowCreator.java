package com.infoclinika.mssharing.workflow;

import com.google.common.base.Optional;
import com.infoclinika.mssharing.workflow.steps.WorkflowStepConfiguration.QueueConfiguration;
import com.infoclinika.tasks.api.workflow.WorkflowStepInput;
import com.infoclinika.tasks.api.workflow.WorkflowStepOutput;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Creates Workflow structure to run them afterwards
 * Each workflow has carcass called WorkflowTemplate.
 * WorkflowTemplate is composed with WorkflowStepTypes with ordering.
 * Each Workflow is composed with WorkflowSteps which represents implementations of WorkflowStepType of WorkflowTemplate
 * <p>
 * Use case how Workflow could be used: user selects WorkflowTemplate, composes each step(WorkflowStepType) with real implementation(WorkflowStep)
 * and this action leads to creating Workflow
 *
 * @author andrii.loboda
 */
@Transactional
public interface WorkflowCreator {
    /**
     * Creates WorkflowTemplate based on WorkflowStepTypes
     *
     * @param actor             user initiated the action
     * @param name              Workflow Template's name
     * @param description       Description of Workflow Template
     * @param workflowStepTypes ordered list of step types
     * @param categoryInStr     experiment category to which this workflow template belongs
     * @param type              type of the workflow template, e.g. dms, maxquant, pecan, etc.
     */
    long createWorkflowTemplate(long actor, String name, String description, List<Long> workflowStepTypes, String categoryInStr, WorkflowType type);

    /**
     * Creates WorkflowStep based on WorkflowStepType
     *
     * @param actor                 user initiated the action
     * @param name                  WorkflowStep name
     * @param description           Description of WorkflowStep
     * @param accessKeyToken        Access token to let remote clients to connect to the application to elicit data from the application
     * @param secretKey             Secret token to let remote clients to connect to the application to elicit data from the application
     * @param workflowStepType      WorkflowStepType this step refers to
     * @param queueTaskConf         Queue configuration for sending "to process" tasks to messaging queue
     * @param queueRemoveTaskConf   Queue configuration for sending "cancel task I've sent before" tasks
     * @param queueEstimateTaskConf Queue configuration for sending "give me the ETA for task" tasks
     * @param processorClass        Class which extends from {@link com.infoclinika.mssharing.workflow.steps.AbstractWorkflowStepProcessor} which represents workflow step processing
     * @param taskClass             Task class which extends from {@link WorkflowStepInput} and represents "to process" task
     * @param taskResultClass       TaskResult class which extends from {@link WorkflowStepOutput} and represents "to process" task result
     * @param uiDescription         UI carcass to specify parameters for this workflow step
     */
    long createWorkflowStep(long actor, String name, String description, String accessKeyToken, String secretKey, long workflowStepType,
                            QueueConfiguration queueTaskConf, Optional<QueueConfiguration> queueRemoveTaskConf, Optional<QueueConfiguration> queueEstimateTaskConf,
                            Class processorClass,
                            Class<? extends WorkflowStepInput> taskClass, Class<? extends WorkflowStepOutput> taskResultClass,
                            String uiDescription);


    /**
     * Creates WorkflowStepType which is basic entity for composition WorkflowTemplate
     *
     * @param actor       user initiated the action
     * @param name        WorkflowStep's name
     * @param description WorkflowStepType's description
     * @param mustSpecify specifies whether this step is mandatory for explicit selection on UI of application while composing the Workflow
     */
    long createWorkflowStepType(long actor, String name, String description, boolean mustSpecify);
}
