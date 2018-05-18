package com.infoclinika.mssharing.model.test.processing;

import com.google.common.collect.ImmutableSet;
import com.infoclinika.mssharing.model.helper.AbstractTest;
import com.infoclinika.mssharing.model.helper.ExperimentSampleItem;
import com.infoclinika.mssharing.model.internal.read.ProcessingFileReader;
import com.infoclinika.mssharing.model.internal.read.ProcessingRunReader;
import com.infoclinika.mssharing.model.read.dto.details.ExperimentItem;
import com.infoclinika.mssharing.model.read.dto.details.FileItem;
import com.infoclinika.mssharing.model.write.ProjectInfo;
import com.infoclinika.mssharing.platform.model.write.ExperimentManagementTemplate;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableList.of;
import static com.infoclinika.mssharing.model.helper.Data.PROJECT_TITLE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


public class ManagingProcessesTest extends AbstractTest{



    @Test
    public void addingProcessingFile(){
        final long userId = uc.createLab3AndBob();
        final long experiment = experiment(userId, uc.getLab3());

        final FileItem file = (FileItem)detailsReader.readExperiment(userId, experiment).files.iterator().next();

        final long processingFileId = createProcessingFile(experiment, file);
        ProcessingFileReader.ProcessingFileInfo processingFileInfo = processingFileReader.readProcessingFileInfo(processingFileId);

        final long experimentId = processingFileInfo.abstractExperiment.getId();

        assertEquals(experiment, experimentId);
        assertEquals(file.name, processingFileInfo.name);
    }


    @Test
    public void associateProcessingFileWithExperimentFile(){

        final long userId = uc.createLab3AndBob();
        final long experiment = experiment(userId, uc.getLab3());

        final FileItem file = (FileItem)detailsReader.readExperiment(userId, experiment).files.iterator().next();
        final long processingFileId = createProcessingFile(experiment, file);

        Map<String, Collection<String>> fileToFileMap = createFileToFileMap(file, processingFileId);
        processingFileManagement.associateProcessingFileWithRawFile(fileToFileMap, experiment, userId, "ProcessingRunTest");

        assertProcessingFilesIsAssociateExperimentFile(processingFileId, file.id);
    }


    @Test
    public void associateMultipartProcessingFilesWithExperimentFiles(){
        final long user = uc.createLab3AndBob();
        final long experiment = experiment(user, uc.getLab3());

        final long instrument = instrumentFromExperimentFile(user, experiment);
        final long file1 = uc.saveFile(user, instrument);
        final long file2 = uc.saveFile(user, instrument);

        final ExperimentSampleItem sample1 = sampleWithFactors(file1, of("1"));
        final ExperimentSampleItem sample2 = sampleWithFactors(file2, of("2"));
        addFilesToExperiment(user, experiment, of(factor(experiment)), of(
                new com.infoclinika.mssharing.model.write.FileItem(file1, false, 0, preparedSample(file1, ImmutableSet.of(sample1))),
                new com.infoclinika.mssharing.model.write.FileItem(file2, false, 0, preparedSample(file2, ImmutableSet.of(sample2)))), of("3"));

        final ExperimentItem experimentItem = detailsReader.readExperiment(user, experiment);
        List<Long> processingFilesList = createMultiProcessingFiles(experimentItem);
        Map<String, Collection<String>> map = createFileToFileMap(experimentItem, processingFilesList);

        processingFileManagement.associateProcessingFileWithRawFile(map, experiment, user, "ProcessingRunTests");

        for(int i = 0; i < processingFilesList.size(); i++){
            assertMultipartProcessingFilesIsAssociateExperimentFile(processingFilesList.get(i), experimentItem.files.get(i).id, experimentItem);
        }
    }


    @Test
    public void createProcessingRunWithoutAssociate(){
        final long user = uc.createLab3AndBob();
        final long experiment = experiment(user, uc.getLab3());

        final long processingRunId = createProcessingRun(experiment, "ProcessingRunWithoutFiles");
        ProcessingRunReader.ProcessingRunInfo processingRunInfo = processingRunReader.readProcessingRun(processingRunId);
        final long processingRunExperiment = processingRunInfo.abstractExperiment.getId();

        assertEquals(experiment, processingRunExperiment);
        assertEquals("ProcessingRunWithoutFiles", processingRunInfo.name);
    }

    @Test
    public void updateProcessingRun(){
        final long user = uc.createLab3AndBob();
        final long experiment = experiment(user, uc.getLab3());

        final long instrument = instrumentFromExperimentFile(user, experiment);
        final long file0 = uc.saveFile(user, instrument);
        final long file1 = uc.saveFile(user, instrument);

        final ExperimentSampleItem sample0 = sampleWithFactors(file0, of("1"));
        final ExperimentSampleItem sample1 = sampleWithFactors(file1, of("2"));
        addFilesToExperiment(user, experiment, of(factor(experiment)), of(
                new com.infoclinika.mssharing.model.write.FileItem(file0, false, 0, preparedSample(file0, ImmutableSet.of(sample0))),
                new com.infoclinika.mssharing.model.write.FileItem(file1, false, 0, preparedSample(file1, ImmutableSet.of(sample1)))), of("3"));

        final ExperimentItem experimentItem = detailsReader.readExperiment(user, experiment);
        List<Long> processingFilesList = createMultiProcessingFiles(experimentItem);
        Map<String, Collection<String>> map = createFileToFileMap(experimentItem, processingFilesList);

        processingFileManagement.associateProcessingFileWithRawFile(map, experiment, user, "UpdateProcessingRun");
        ProcessingRunReader.ProcessingRunInfo processingRunInfo = processingRunReader.readProcessingRunByNameAndExperiment(experiment, "UpdateProcessingRun");
        long experimentId = processingRunInfo.abstractExperiment.getId();

        assertEquals(processingFilesList.size(), processingRunInfo.processingFiles.size());
        assertEquals(experiment, experimentId);
        assertEquals("UpdateProcessingRun", processingRunInfo.name);


        generateFilesToExperiment(user, instrument, experiment);

        final ExperimentItem experimentItems = detailsReader.readExperiment(user, experiment);
        List<Long> newProcessingFiles = createMultiProcessingFiles(experimentItems);
        Map<String, Collection<String>> newAssociationFilesMap = createFileToFileMap(experimentItems, newProcessingFiles);


        processingFileManagement.associateProcessingFileWithRawFile(newAssociationFilesMap, experiment, user, "UpdateProcessingRun");
        ProcessingRunReader.ProcessingRunInfo processingRun = processingRunReader.readProcessingRunByNameAndExperiment(experiment,"UpdateProcessingRun");
        assertTrue(processingRun.processingFiles.size() == 10);
    }



    private long experiment(long user, long lab) {
        final long project = studyManagement.createProject(user, new ProjectInfo(PROJECT_TITLE, "DNA", "Some proj", lab));
        return createInstrumentAndExperimentWithOneFile(user, lab, project);
    }

    private ExperimentManagementTemplate.MetaFactorTemplate factor(long experimentId) {
        return new ExperimentManagementTemplate.MetaFactorTemplate(generateString(), "", false, experimentId);
    }


    private void generateFilesToExperiment(long user, long instrument, long experiment){

        final long file3 = uc.saveFile(user, instrument);
        final long file4 = uc.saveFile(user, instrument);
        final long file5 = uc.saveFile(user, instrument);
        final long file6 = uc.saveFile(user, instrument);

        final ExperimentSampleItem sample3 = sampleWithFactors(file3, of("1"));
        final ExperimentSampleItem sample4 = sampleWithFactors(file4, of("2"));

        final ExperimentSampleItem sample5 = sampleWithFactors(file5, of("3"));
        final ExperimentSampleItem sample6 = sampleWithFactors(file6, of("4"));

        addFilesToExperiment(user, experiment, of(factor(experiment)), of(
                new com.infoclinika.mssharing.model.write.FileItem(file3, false, 0, preparedSample(file3, ImmutableSet.of(sample3))),
                new com.infoclinika.mssharing.model.write.FileItem(file4, false, 0, preparedSample(file4, ImmutableSet.of(sample4)))), of("3"));
        addFilesToExperiment(user, experiment, of(factor(experiment)), of(
                new com.infoclinika.mssharing.model.write.FileItem(file5, false, 0, preparedSample(file5, ImmutableSet.of(sample5))),
                new com.infoclinika.mssharing.model.write.FileItem(file6, false, 0, preparedSample(file6, ImmutableSet.of(sample6)))), of("6"));

    }
}
