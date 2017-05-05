package com.infoclinika.mssharing.workflow.steps.preparator;

import com.infoclinika.mssharing.workflow.WorkflowRun;
import com.infoclinika.tasks.api.workflow.input.ProteinSearchTask;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The class which implements this interface should be able to prepare workflow task for further execution.
 * This is one of the 3 major parts of WorkflowStep execution: preparing tasks, execute them, persist them.
 *
 * @author andrii.loboda
 */

@Transactional
public interface WorkflowStepTaskPreparator<T extends ProteinSearchTask> {
    /**
     * Creates list of tasks to execute further based on provided {@link WorkflowRun}
     * Appropriate for use to prepare single and multi tasks(just return list with only one item)
     */
    List<T> prepare(WorkflowRun workflowRun);
}
