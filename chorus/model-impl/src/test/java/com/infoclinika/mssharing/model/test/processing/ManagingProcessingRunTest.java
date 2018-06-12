package com.infoclinika.mssharing.model.test.processing;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.infoclinika.mssharing.model.helper.ExperimentLabelsInfo;
import com.infoclinika.mssharing.model.helper.ExperimentSampleItem;
import com.infoclinika.mssharing.model.internal.entity.ExperimentSample;
import com.infoclinika.mssharing.model.internal.entity.ProcessingFile;
import com.infoclinika.mssharing.model.internal.read.ProcessingRunReader;
import com.infoclinika.mssharing.model.read.dto.details.ExperimentItem;
import com.infoclinika.mssharing.model.write.AnalysisBounds;
import com.infoclinika.mssharing.model.write.ExperimentInfo;
import com.infoclinika.mssharing.platform.model.read.DetailsReaderTemplate;
import com.infoclinika.mssharing.platform.model.write.ExperimentManagementTemplate;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.infoclinika.mssharing.model.helper.ExperimentSampleTypeItem.HEAVY;
import static com.infoclinika.mssharing.model.helper.ExperimentSampleTypeItem.LIGHT;
import static com.infoclinika.mssharing.model.helper.ExperimentSampleTypeItem.MEDIUM;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


public class ManagingProcessingRunTest extends AbstractProcessingTest{

    @Test
    public void testUpdateProcessingRunAfterFirstCreation(){
        final long user = uc.createLab3AndBob();
        final long experiment = createExperimentWithOneRawFile(user, uc.getLab3());

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

        processingFileManagement.associateProcessingFileWithRawFile(map, new HashMap<>(),experiment, user, "UpdateProcessingRun");
        ProcessingRunReader.ProcessingRunInfo processingRunInfo = processingRunReader.readProcessingRunByNameAndExperiment(experiment, "UpdateProcessingRun");

        assertEquals(processingFilesList.size(), processingRunInfo.processingFiles.size());
        assertEquals("UpdateProcessingRun", processingRunInfo.name);

        generateFilesToExperiment(user, instrument, experiment);

        final ExperimentItem experimentItems = detailsReader.readExperiment(user, experiment);
        List<Long> newProcessingFiles = createMultiProcessingFiles(experimentItems);
        Map<String, Collection<String>> newAssociationFilesMap = createFileToFileMap(experimentItems, newProcessingFiles);

        processingFileManagement.associateProcessingFileWithRawFile(newAssociationFilesMap, new HashMap<>(),experiment, user, "UpdateProcessingRun");
        ProcessingRunReader.ProcessingRunInfo processingRun = processingRunReader.readProcessingRunByNameAndExperiment(experiment,"UpdateProcessingRun");

        assertTrue(processingRun.processingFiles.size() == 8);
        assertEquals(processingRunInfo.id, processingRun.id);
    }


    @Test
    public void createProcessingRunWithAssociateFiles(){

        final long user = uc.createLab3AndBob();
        final long experiment = createExperimentWithOneRawFile(user, uc.getLab3());

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
        Map<String, Collection<String>> fileToFileMap = createFileToFileMap(experimentItem, processingFilesList);

        processingFileManagement.associateProcessingFileWithRawFile(fileToFileMap, new HashMap<>(),experiment, user, "ProcessingRun");

        ProcessingRunReader.ProcessingRunInfo processingRunInfo = processingRunReader.readProcessingRunByNameAndExperiment(experiment, "ProcessingRun");

        final long processingRunExperiment = processingRunInfo.abstractExperiment.getId();

        assertEquals(experiment, processingRunExperiment);
        assertEquals(processingFilesList.size(), processingRunInfo.processingFiles.size());
        assertEquals("ProcessingRun", processingRunInfo.name);
    }


    @Test
    public void createNewSamplesFileMapInProcessingRun(){

        final long bob = uc.createLab3AndBob();
        final long project = uc.createProject(bob);

        long experiment = createExperiment(bob, project);
        final long instrument = instrumentFromExperimentFile(bob, experiment);
        generateFilesToExperiment(bob, instrument, experiment);

        final ExperimentItem experimentItem = detailsReader.readExperiment(bob, experiment);
        final DetailsReaderTemplate.ExperimentShortInfo shortInfo = detailsReader.readExperimentShortInfo(bob, experiment);

        List<Long> processingFilesList = createMultiProcessingFiles(experimentItem);
        Map<String, Collection<String>> fileToFileMap = createFileToFileMap(experimentItem, processingFilesList);
        Map<String, Collection<String>> sampleFileMap = createSamplesToFileMap(shortInfo, fileToFileMap);

        processingFileManagement.associateProcessingFileWithRawFile(fileToFileMap, sampleFileMap ,experiment, bob, "ProcessingRunWithSample");
        ProcessingRunReader.ProcessingRunInfo processingRunInfo = processingRunReader.readProcessingRunByNameAndExperiment(experiment, "ProcessingRunWithSample");
        HashMultimap<String, String> sample = extractSampleToFile(processingRunInfo);

        assertEquals(sample.size(), 3);
        assertEquals(sample.get("sample_light_1").size(), 1);
        assertEquals(sample.get("sample_light_2").size(), 1);
        assertTrue(sample.get("sample_light_2").contains(sampleFileMap.get("sample_light_2").iterator().next()));
        assertTrue(sample.get("sample_light_1").contains(sampleFileMap.get("sample_light_1").iterator().next()));

    }


    private static HashMultimap<String, String> extractSampleToFile(ProcessingRunReader.ProcessingRunInfo processingRunInfo){

        HashMultimap<String, String> sampleToFiles = HashMultimap.create();

        for(ProcessingFile file : processingRunInfo.processingFiles){
            for(ExperimentSample experimentSample : file.getExperimentSamples()){
                sampleToFiles.put(experimentSample.getName(), file.getName());
            }
        }
        return sampleToFiles;
    }


    private void generateFilesToExperiment(long user, long instrument, long experiment){

        final long file3 = uc.saveFile(user, instrument);
        final long file4 = uc.saveFile(user, instrument);

        final ExperimentSampleItem sample3 = new ExperimentSampleItem("sample_light_1", LIGHT, of("sample_one"));
        final ExperimentSampleItem sample4 = new ExperimentSampleItem("sample_light_2", LIGHT, of("sample_two"));

        addFilesToExperiment(user, experiment, of(factor(experiment)), of(
                new com.infoclinika.mssharing.model.write.FileItem(file3, false, 0, preparedSample(file3, ImmutableSet.of(sample3))),
                new com.infoclinika.mssharing.model.write.FileItem(file4, false, 0, preparedSample(file4, ImmutableSet.of(sample4)))), of("3"));

    }

    protected ExperimentManagementTemplate.MetaFactorTemplate factor(long experimentId) {
        return new ExperimentManagementTemplate.MetaFactorTemplate(generateString(), "", false, experimentId);
    }

}
