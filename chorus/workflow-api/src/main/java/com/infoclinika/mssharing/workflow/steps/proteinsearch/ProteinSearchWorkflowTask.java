package com.infoclinika.mssharing.workflow.steps.proteinsearch;

import com.infoclinika.tasks.api.workflow.input.ProteinSearchTask;
import com.infoclinika.tasks.api.workflow.model.SearchParams;
import com.infoclinika.tasks.api.workflow.output.ProteinSearchTaskResult;

/**
 * @author andrii.loboda
 */
public class ProteinSearchWorkflowTask extends ProteinSearchTask {
    public boolean runOnlyNotComplete;
    public ProteinSearchWorkflowTask(long proteinSearch, ProteinSearchTaskResult<? extends ProteinSearchTask> previousResult, SearchParams searchParams, long workflowStepType, String uploadKeyPrefix,  boolean runOnlyNotComplete) {
        super(proteinSearch, previousResult, searchParams, workflowStepType, uploadKeyPrefix);
        this.runOnlyNotComplete = runOnlyNotComplete;
    }

    public ProteinSearchWorkflowTask() {
    }
}
