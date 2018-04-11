package com.infoclinika.mssharing.web.controller.v2.service;


import com.google.common.base.Joiner;
import com.infoclinika.analysis.storage.cloud.CloudStorageFactory;
import com.infoclinika.analysis.storage.cloud.CloudStorageItemReference;
import com.infoclinika.analysis.storage.cloud.CloudStorageService;
import com.infoclinika.mssharing.model.write.InstrumentManagement;
import com.infoclinika.mssharing.platform.fileserver.model.NodePath;
import lombok.Data;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Service
public class UploadFileService {

    private static final Logger LOGGER = Logger.getLogger(UploadFileService.class);

    private static final String DELIMETER = "/";

    @Inject
    private InstrumentManagement instrumentManagement;

    @Inject
    private AwsConfigService awsConfigService;

    private static final CloudStorageService CLOUD_STORAGE_SERVICE = CloudStorageFactory.service();


    public ResponseEntity<Object> uploadFileToStorage(long userId, long instrumentId, MultipartFile multipartFile) throws IOException{

        File file = convertMultipartToFile(multipartFile);
        boolean fileExistAnInstrument = instrumentManagement.isFileAlreadyUploadedForInstrument(userId, instrumentId, file.getName());

        final NodePath nodePath = new NodePath(Joiner.on(DELIMETER).join(awsConfigService.getTargetFolder(), userId, instrumentId + DELIMETER));

        CloudStorageItemReference itemReference = awsConfigService.storageItemReference(nodePath.getPath() + file.getName());

        if(fileExistAnInstrument && CLOUD_STORAGE_SERVICE.existsAtCloud(itemReference)){
            LOGGER.info("File with name key: " + nodePath.getPath() + file.getName() + " already exists");

            return new ResponseEntity("File with name key: " + nodePath.getPath() + file.getName() + " already exists", HttpStatus.OK);
        }else {
            CloudStorageItemReference cloudStorageItemReference = awsConfigService.storageItemReference(nodePath.getPath());
            CLOUD_STORAGE_SERVICE.uploadToCloud(file, cloudStorageItemReference.getBucket(), cloudStorageItemReference.getKey());
            LOGGER.info("File was uploaded to storage");

            return new ResponseEntity("Upload complete:  " + nodePath.getPath() + file.getName(), HttpStatus.OK);
        }
    }

    private File convertMultipartToFile(MultipartFile multipartFile) throws IOException{
        File result = new File(multipartFile.getOriginalFilename());
        FileOutputStream fileOutputStream = new FileOutputStream(result);
        fileOutputStream.write(multipartFile.getBytes());
        fileOutputStream.close();
        return result;
    }






}
