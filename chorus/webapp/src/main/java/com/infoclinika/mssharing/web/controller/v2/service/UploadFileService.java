package com.infoclinika.mssharing.web.controller.v2.service;

import com.infoclinika.analysis.storage.cloud.CloudStorageFactory;
import com.infoclinika.analysis.storage.cloud.CloudStorageItemReference;
import com.infoclinika.analysis.storage.cloud.CloudStorageService;
import com.infoclinika.mssharing.model.internal.s3client.AwsS3ClientConfigurationService;
import com.infoclinika.mssharing.model.read.DetailsReader;
import com.infoclinika.mssharing.model.read.ProcessingRunReader;
import com.infoclinika.mssharing.model.write.ProcessingFileManagement;
import com.infoclinika.mssharing.platform.fileserver.model.NodePath;
import com.infoclinika.mssharing.platform.model.read.DetailsReaderTemplate;
import com.infoclinika.mssharing.web.controller.v2.dto.ProcessingRunsDTO;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@Service
@Transactional
public class UploadFileService {

    private static final Logger LOGGER = Logger.getLogger(UploadFileService.class);

    private static final CloudStorageService CLOUD_STORAGE_SERVICE = CloudStorageFactory.service();

    @Inject
    private ProcessingFileManagement processingFileManagement;
    @Inject
    private AwsS3ClientConfigurationService awsConfigService;
    @Inject
    private RestAuthClientService restAuthClientService;
    @Inject
    private DetailsReader detailsReader;
    @Inject
    private ProcessingRunReader processingRunReader;


    public ResponseEntity<Object> uploadFileToStorage(long user, long experimentId, MultipartFile[] multipartFiles) throws IOException{
        Map<String, Collection<String>> resultsProcessingFiles = new HashMap();
        List<String> uploadComplete = new ArrayList();
        List<String> uploadErrors = new ArrayList();

        boolean isUserLabMembership = restAuthClientService.isUserLabMembership(user, experimentId);
        final DetailsReaderTemplate.ExperimentShortInfo experimentShortInfo = detailsReader.readExperimentShortInfo(user, experimentId);
        if(isUserLabMembership && experimentShortInfo.files.size() > 0){

            if(multipartFiles.length > 0){
                for (MultipartFile multipartFile: multipartFiles) {

                    File file = convertMultipartToFile(multipartFile);

                    startUploadFilesToStorage(experimentId, file, resultsProcessingFiles,uploadComplete, uploadErrors);

                    LOGGER.info(file.getAbsolutePath() + "startUploadFilesToStorage");
                }

                return new ResponseEntity(resultsProcessingFiles.toString(), HttpStatus.OK);
//                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resultsProcessingFiles.toString());
            }
        }
        return new ResponseEntity("User with ID: " + user + "does not have access to lab", HttpStatus.UNAUTHORIZED);
    }




    private File convertMultipartToFile(MultipartFile multipartFile) throws IOException{
        File result = new File("/home/admin-infoclinika/" + multipartFile.getOriginalFilename());
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


    private void startUploadFilesToStorage(long experiment, File file, Map<String, Collection<String>> map,List<String> uploadDone, List<String> uploadExists){

        boolean processingFileAlreadyExist = processingFileManagement.isProcessingFileAlreadyUploadedToExperiment(experiment, file.getName());
        final NodePath nodePath = awsConfigService.returnProcessingStorageTargetFolder(experiment, file.getName());
        final CloudStorageItemReference itemReference = awsConfigService.storageItemReference(nodePath.getPath());

        if(!processingFileAlreadyExist && !CLOUD_STORAGE_SERVICE.existsAtCloud(itemReference)){

            CLOUD_STORAGE_SERVICE.uploadToCloud(file, itemReference.getBucket(), itemReference.getKey());
            final ProcessingFileManagement.ProcessingFileShortInfo processingFileShortInfo = new ProcessingFileManagement.ProcessingFileShortInfo(file.getName(), itemReference.getKey());
            processingFileManagement.createProcessingFile(experiment, processingFileShortInfo);

            uploadDone.add(file.getName());
            map.put("uploadComplete", uploadDone);

            LOGGER.info("Processing file  was uploaded to storage: " + nodePath.getPath());

        }else {
            uploadExists.add(file.getName());
            map.put("alreadyExists", uploadExists);

            LOGGER.info("Processing file with name key: " + nodePath.getPath() + " already exists");
        }
    }

}
