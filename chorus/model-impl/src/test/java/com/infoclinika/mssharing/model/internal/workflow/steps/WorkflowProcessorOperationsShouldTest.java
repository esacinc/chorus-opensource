//package com.infoclinika.mssharing.model.internal.workflow.steps;
//
//import com.google.common.base.Predicate;
//import com.google.common.collect.Iterables;
//import com.infoclinika.mssharing.model.helper.AbstractTest;
//import com.infoclinika.mssharing.model.internal.workflow.steps.proteinsearch.persister.api.ImageProcessingTaskResultPersister;
//import com.infoclinika.mssharing.model.internal.workflow.steps.proteinsearch.preparator.api.ImageProcessingTaskPreparator;
//import com.infoclinika.mssharing.model.read.proteinsearch.WorkflowStepReader;
//import com.infoclinika.mssharing.model.read.proteinsearch.WorkflowStepReader.RunWorkflowExecutionItem;
//import com.infoclinika.mssharing.model.read.proteinsearch.WorkflowStepReader.RunWorkflowStepItem;
//import com.infoclinika.mssharing.workflow.WorkflowRun;
//import com.infoclinika.mssharing.workflow.WorkflowRun.WorkflowStepItem;
//import com.infoclinika.mssharing.workflow.WorkflowRunner;
//import com.infoclinika.mssharing.workflow.steps.WorkflowStepConfiguration;
//import com.infoclinika.mssharing.workflow.steps.proteinsearch.PersistProteinDatabaseStepTypeProcessor;
//import com.infoclinika.tasks.api.workflow.input.ImageProcessingTask;
//import com.infoclinika.tasks.api.workflow.input.ProteinSearchTask;
//import com.infoclinika.tasks.api.workflow.output.ImageProcessingTaskResult;
//import com.infoclinika.tasks.api.workflow.output.ProteinSearchTaskResult;
//import org.testng.annotations.Test;
//
//import javax.inject.Inject;
//
//import static com.google.common.collect.Lists.newArrayList;
//import static com.infoclinika.mssharing.model.read.proteinsearch.RunReader.RunStatus.COMPLETED;
//import static com.infoclinika.mssharing.model.read.proteinsearch.RunReader.RunStatus.NOT_STARTED;
//import static org.testng.Assert.assertEquals;
//
///**
// * @author andrii.loboda
// */
//public class WorkflowProcessorOperationsShouldTest extends AbstractTest {
//    private static final int minKoef = 60 * 1000;
//    @Inject
//    private WorkflowProcessorOperations workflowProcessorOperations;
//    @Inject
//    private WorkflowRunner workflowRunner;
//    @Inject
//    private WorkflowStepReader workflowStepReader;
//
//    @Test
//    public void check_run_in_progress_not_throw_an_exception() {
//        setProteinSearch(true);
//        initializeWorkflowsForRuns();
//        final long bob = uc.createLab3AndBob();
//        final long ex = createExperimentForRun(bob);
//        final long proteinSearch = createProteinSearch(bob, ex);
//        workflowRunner.run(bob, proteinSearch);
//
//        workflowProcessorOperations.checkRunInProgress(proteinSearch);
//    }
//
//    @Test(expectedExceptions = IllegalStateException.class)
//    public void throw_exception_if_run_not_in_progress() {
//        setProteinSearch(true);
//        initializeWorkflowsForRuns();
//        final long bob = uc.createLab3AndBob();
//        final long project = uc.createProject(bob, uc.getLab3());
//        final long experiment = createExperiment(bob, project);
//        final long proteinSearch = createProteinSearch(bob, experiment);
//
//        workflowProcessorOperations.checkRunInProgress(proteinSearch);
//    }
//
//    @Test
//    public void read_configuration_of_workflow_step() {
//        initializeWorkflowsForRuns();
//        setProteinSearch(true);
//        final long bob = uc.createLab3AndBob();
//        final long project = uc.createProject(bob, uc.getLab3());
//        long instrument = createInstrumentAndApproveIfNeeded(bob, uc.getLab3());
//        long fileId = uc.saveFile(bob, instrument);
//        final long experiment = createExperiment(bob, project, uc.getLab3(), noFactoredFile(fileId));
//        translateFileForRun(bob, fileId, experiment);
//        final long imageProcessingStepType = getImageProcessingStepType();
//        final long newWorkflowStep = createWorkflowStep(imageProcessingStepType,
//                new WorkflowStepConfiguration.QueueConfiguration("someHost", -12, "noname", "no password", "wrong-queue", 2000),
//                ImageProcessingTaskPreparator.class, ImageProcessingTaskResultPersister.class, ImageProcessingTask.class, ImageProcessingTaskResult.class);
//        final long proteinSearch = createProteinSearch(bob, experiment, getProteinDatabaseEcoli(),
//                getSingleStepTemplate(), newArrayList(newWorkflowStep));
//
//        final WorkflowStepConfiguration obtainedConfiguration = workflowProcessorOperations.getConfiguration(
//                new WorkflowRun(proteinSearch, new WorkflowStepItem(imageProcessingStepType, "empty step", null, -1, -1, false), false, null));
//
//        assertEquals(obtainedConfiguration.taskQueueConfiguration.hostName, "someHost");
//        assertEquals(obtainedConfiguration.taskQueueConfiguration.password, "no password");
//    }
//
//    @Test
//    public void change_last_step_to_next_in_workflow() {
//        initializeWorkflowsForRuns();
//        setProteinSearch(true);
//        final long bob = uc.createLab3AndBob();
//        final long project = uc.createProject(bob, uc.getLab3());
//        long instrument = createInstrumentAndApproveIfNeeded(bob, uc.getLab3());
//        long fileId = uc.saveFile(bob, instrument);
//        final long experiment = createExperiment(bob, project, uc.getLab3(), noFactoredFile(fileId));
//        translateFileForRun(bob, fileId, experiment);
//        final long proteinSearch = createProteinSearch(bob, experiment, getProteinDatabaseEcoli(), getDmsTemplate(), getDmsSteps());
//
//        final RunWorkflowExecutionItem workflowExecutionItem = workflowStepReader.readRunWorkflowSteps(bob, proteinSearch);
//        assertEquals(getPersistProteinDatabaseWorkflowInfo(workflowExecutionItem).status, NOT_STARTED);
//
//        final WorkflowRun workflowRun = new WorkflowRun(proteinSearch, new WorkflowStepItem(getPersistProteinDBStepType(), "Persist db step", null, -1L, -1L, false), false, null);
//        workflowProcessorOperations.incrementLastWorkflowStep(new ProteinSearchTaskResult<ProteinSearchTask>(
//                new ProteinSearchTask(proteinSearch, null, null, getPersistProteinDBStepType(), null), 100),
//                workflowRun);
//
//        final RunWorkflowExecutionItem workflowExecutionItem2 = workflowStepReader.readRunWorkflowSteps(bob, proteinSearch);
//        assertEquals(getPersistProteinDatabaseWorkflowInfo(workflowExecutionItem2).status, COMPLETED);
//    }
//
//    @Test
//    public void persist_duration_during_execution() {
//        initializeWorkflowsForRuns();
//        setProteinSearch(true);
//        final long bob = uc.createLab3AndBob();
//        final long project = uc.createProject(bob, uc.getLab3());
//        long instrument = createInstrumentAndApproveIfNeeded(bob, uc.getLab3());
//        long fileId = uc.saveFile(bob, instrument);
//        final long experiment = createExperiment(bob, project, uc.getLab3(), noFactoredFile(fileId));
//        translateFileForRun(bob, fileId, experiment);
//        final long run = createProteinSearch(bob, experiment, getProteinDatabaseEcoli(), getDmsTemplate(), getDmsSteps());
//
//
//        workflowProcessorOperations.persistStatsDuringExecution(new ProteinSearchTaskResult(
//                new ProteinSearchTask(run, null, null, getImageProcessingStepType(), null), 10 * minKoef), 20 * minKoef);
//        workflowProcessorOperations.persistStatsDuringExecution(new ProteinSearchTaskResult(
//                new ProteinSearchTask(run, null, null, getImageProcessingStepType(), null), 20 * minKoef), 50 * minKoef);
//
//        final RunWorkflowExecutionItem workflowSteps = workflowStepReader.readRunWorkflowSteps(bob, run);
//        final RunWorkflowStepItem stepinfo = getImageProcessingWorkflowInfo(workflowSteps);
//        assertEquals(stepinfo.statistic.pureTaskExecutionDurationInMin, 30);
//        assertEquals(stepinfo.statistic.totalTaskExecutionDurationInMin, 50);
//        assertEquals(stepinfo.queue.completedNumberOfTasks, 2);
//    }
//
//    private RunWorkflowStepItem getImageProcessingWorkflowInfo(RunWorkflowExecutionItem workflowSteps) {
//        return Iterables.find(workflowSteps.workflowSteps, new Predicate<RunWorkflowStepItem>() {
//            @Override
//            public boolean apply(RunWorkflowStepItem input) {
//                return input.id == getImageProcessingStep();
//            }
//        });
//    }
//
//    private RunWorkflowStepItem getPersistProteinDatabaseWorkflowInfo(RunWorkflowExecutionItem workflowSteps) {
//        return Iterables.find(workflowSteps.workflowSteps, new Predicate<RunWorkflowStepItem>() {
//            @Override
//            public boolean apply(RunWorkflowStepItem input) {
//                return input.id == getPersistProteinDBStep();
//            }
//        });
//    }
//
//
//    @Test
//    public void persist_execution_time() {
//        initializeWorkflowsForRuns();
//        setProteinSearch(true);
//        final long bob = uc.createLab3AndBob();
//        final long project = uc.createProject(bob, uc.getLab3());
//        long instrument = createInstrumentAndApproveIfNeeded(bob, uc.getLab3());
//        long fileId = uc.saveFile(bob, instrument);
//        final long experiment = createExperiment(bob, project, uc.getLab3(), noFactoredFile(fileId));
//        translateFileForRun(bob, fileId, experiment);
//        final long run = createProteinSearch(bob, experiment, getProteinDatabaseEcoli(), getDmsTemplate(), getDmsSteps());
//
//        workflowProcessorOperations.persistStatsExecutionTime(new ProteinSearchTask(run, null, null, getImageProcessingStepType(), null), 202 * minKoef);
//
//        final RunWorkflowExecutionItem workflowSteps = workflowStepReader.readRunWorkflowSteps(bob, run);
//        final RunWorkflowStepItem stepinfo = getImageProcessingWorkflowInfo(workflowSteps);
//        assertEquals(stepinfo.statistic.totalTaskExecutionDurationInMin, 202);
//
//    }
//
//    @Test
//    public void perstist_total_time() {
//        initializeWorkflowsForRuns();
//        setProteinSearch(true);
//        final long bob = uc.createLab3AndBob();
//        final long project = uc.createProject(bob, uc.getLab3());
//        long instrument = createInstrumentAndApproveIfNeeded(bob, uc.getLab3());
//        long fileId = uc.saveFile(bob, instrument);
//        final long experiment = createExperiment(bob, project, uc.getLab3(), noFactoredFile(fileId));
//        translateFileForRun(bob, fileId, experiment);
//        final long run = createProteinSearch(bob, experiment, getProteinDatabaseEcoli(), getDmsTemplate(), getDmsSteps());
//
//        workflowProcessorOperations.persistStatsTotalTime(new ProteinSearchTask(run, null, null, getImageProcessingStepType(), null), 198 * minKoef);
//
//        final RunWorkflowExecutionItem workflowSteps = workflowStepReader.readRunWorkflowSteps(bob, run);
//        final RunWorkflowStepItem stepinfo = getImageProcessingWorkflowInfo(workflowSteps);
//        assertEquals(stepinfo.statistic.totalDurationInMin, 198);
//    }
//
//}
