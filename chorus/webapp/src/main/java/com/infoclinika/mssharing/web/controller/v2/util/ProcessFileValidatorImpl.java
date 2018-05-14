package com.infoclinika.mssharing.web.controller.v2.util;


import com.infoclinika.mssharing.model.internal.entity.ProcessingFile;
import com.infoclinika.mssharing.model.internal.read.ProcessingFileReader;
import com.infoclinika.mssharing.model.internal.repository.FileMetaDataRepository;
import com.infoclinika.mssharing.model.read.DetailsReader;
import com.infoclinika.mssharing.model.read.dto.details.ExperimentItem;
import static com.infoclinika.mssharing.dto.FunctionTransformerAbstract.toListDto;
import static com.infoclinika.mssharing.web.transform.DtoTransformer.TO_PROCESSING_FILE_DTO;

import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.*;

@Component
public class ProcessFileValidatorImpl implements ProcessFileValidator {

    @Inject
    private DetailsReader detailsReader;
    @Inject
    private FileMetaDataRepository fileMetaDataRepository;
    @Inject
    private ProcessingFileReader processingFileReader;


    @Override
    public Map<String, Collection<String>> validateAssociateFiles(Map<String, Collection<String>> map, long experimentId, long user) {
        Map<String, Collection<String>> collectionMap = new HashMap();
        Collection<String> collection = new ArrayList();
        ExperimentItem experimentItem = detailsReader.readExperiment(user, experimentId);

        for(Map.Entry<String, Collection<String>> entry : map.entrySet()){

            Collection<String> experimentFiles = entry.getValue();

            for(String fileName : experimentFiles) {
                boolean activeFileMetaData = fileMetaDataRepository.findNameByInstrument(experimentItem.instrument.get(), fileName);

                if(!activeFileMetaData){
                    collection.add(fileName);
                    collectionMap.put("error_files", collection);
                }
            }
        }

        return collectionMap;
    }

    @Override
    public Map<String, Collection<String>> validateAllProcessedFilesByExperiment(Map<String, Collection<String>> map, long experiment) {

        List<ProcessingFile> files = processingFileReader.readAllByExperiment(experiment);
        List<ProcessingFileReader.ProcessingFileInfo> processingFileInfos = toListDto(files, TO_PROCESSING_FILE_DTO);
        Set<String> strings = map.keySet();

        Map<String, Collection<String>> results = new HashMap<>();
        Collection<String> collection = new ArrayList<>();

        for(ProcessingFileReader.ProcessingFileInfo processingFileInfo : processingFileInfos){

            for(String name : strings){

                if(processingFileInfo.name.equals(name)){
                    if(!collection.contains(name)){
                        collection.add(name);
                        results.put("not_exist_files", collection);
                    }
                }
            }
        }
        return results;
    }


}
