package com.infoclinika.mssharing.web.controller.v2.service;

import com.infoclinika.analysis.storage.cloud.CloudStorageFactory;
import com.infoclinika.analysis.storage.cloud.CloudStorageItemReference;
import com.infoclinika.analysis.storage.cloud.CloudStorageService;
import com.infoclinika.mssharing.model.read.DetailsReader;
import com.infoclinika.mssharing.model.read.ProcessingRunReader;
import com.infoclinika.mssharing.model.write.ProcessingFileManagement;
import com.infoclinika.mssharing.platform.fileserver.model.NodePath;
import com.infoclinika.mssharing.platform.model.read.DetailsReaderTemplate;
import com.infoclinika.mssharing.web.controller.v2.dto.ProcessingRunsDTO;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Service
@Transactional
public class UploadFileService {

    private static final Logger LOGGER = Logger.getLogger(UploadFileService.class);

    private static final CloudStorageService CLOUD_STORAGE_SERVICE = CloudStorageFactory.service();

    @Inject
    private ProcessingFileManagement processingFileManagement;
    @Inject
    private AwsConfigService awsConfigService;
    @Inject
    private RestAuthClientService restAuthClientService;
    @Inject
    private DetailsReader detailsReader;
    @Inject
    private ProcessingRunReader processingRunReader;


    public ResponseEntity<Object> uploadFileToStorage(long user, long experimentId, MultipartFile multipartFile) throws IOException{

        boolean isUserLabMembership = restAuthClientService.isUserLabMembership(user, experimentId);
        final DetailsReaderTemplate.ExperimentShortInfo experimentShortInfo = detailsReader.readExperimentShortInfo(user, experimentId);

        if(isUserLabMembership && experimentShortInfo.files.size() > 0){

            File file = convertMultipartToFile(multipartFile);
            boolean processingFileAlreadyExist = processingFileManagement.isProcessingFileAlreadyUploadedToExperiment(experimentId, file.getName());
            final NodePath nodePath = awsConfigService.returnProcessingStorageTargetFolder(experimentId, file.getName());
            final CloudStorageItemReference itemReference = awsConfigService.storageItemReference(nodePath.getPath());

            if(!processingFileAlreadyExist && !CLOUD_STORAGE_SERVICE.existsAtCloud(itemReference)){

                CLOUD_STORAGE_SERVICE.uploadToCloud(file, itemReference.getBucket(), itemReference.getKey());
                final ProcessingFileManagement.ProcessingFileInfo processingFileInfo = new ProcessingFileManagement.ProcessingFileInfo(file.getName(), itemReference.getKey());
                processingFileManagement.createProcessingFile(experimentId, processingFileInfo);

                LOGGER.info("Processing file  was uploaded to storage: " + nodePath.getPath());
                return new ResponseEntity("Upload complete:  " + nodePath.getPath(), HttpStatus.OK);

            }else {
                LOGGER.info("Processing file with name key: " + nodePath.getPath() + " already exists");
                return new ResponseEntity("Processing file  with name key: " + nodePath.getPath() + " already exists", HttpStatus.OK);
            }

        }else {
            return new ResponseEntity("User with ID: " + user + "does not have access to lab", HttpStatus.UNAUTHORIZED);
        }
    }

    private File convertMultipartToFile(MultipartFile multipartFile) throws IOException{
        File result = new File(multipartFile.getOriginalFilename());
        FileOutputStream fileOutputStream = new FileOutputStream(result);
        fileOutputStream.write(multipartFile.getBytes());
        fileOutputStream.close();
        return result;
    }


    public ResponseEntity<Object> createProcessingRun(ProcessingRunsDTO dto, long user, long experiment){

        boolean isUserLabMembership = restAuthClientService.isUserLabMembership(user, experiment);
        boolean isProcessingRunAlreadyExist  = processingRunReader.findByProcessingRunName(dto.getName(), experiment);

        if(!isProcessingRunAlreadyExist){
            if(isUserLabMembership){
                processingFileManagement.associateProcessingFileWithRawFile(dto.getFileToFileMap(), experiment, user, dto.getName());
                return new ResponseEntity("Processing Run with name: " + dto.getName() + " successfully created", HttpStatus.OK);
            }else {
                return new ResponseEntity("User with ID: " + user + "does not have access to lab", HttpStatus.UNAUTHORIZED);
            }
        }else {
            return new ResponseEntity("Processing Run with name: "+ dto.getName() +" already exists", HttpStatus.BAD_REQUEST);
        }
    }





}
