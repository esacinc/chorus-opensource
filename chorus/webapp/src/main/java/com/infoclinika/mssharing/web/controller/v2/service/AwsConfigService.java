package com.infoclinika.mssharing.web.controller.v2.service;


import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.common.base.Joiner;
import com.infoclinika.analysis.storage.cloud.CloudStorageItemReference;
import com.infoclinika.mssharing.platform.fileserver.model.NodePath;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;


@Configuration
@Service
public class AwsConfigService {

    private static final Logger LOGGER = Logger.getLogger(AwsConfigService.class);

    private static final String DELIMETER = "/";

    @Value("${processed.files.target.folder}")
    private String processedTargetFolder;

    @Value("${raw.files.target.folder}")
    private String rawFileTargetFolder;

    @Value("${amazon.key}")
    private String accessKeyId;

    @Value("${amazon.secret}")
    private String secretAccessKey;

    @Value("${amazon.active.bucket}")
    private String activeBucket;

    public String getActiveBucket() {
        return activeBucket;
    }

    public String getProcessedTargetFolder() {
        return processedTargetFolder;
    }

    public String getRawFileTargetFolder() {
        return rawFileTargetFolder;
    }

    public BasicAWSCredentials awsCredentialsProvider(){
        return credentials(accessKeyId, secretAccessKey);
    }

    public BasicAWSCredentials credentials(String accessKeyId, String secretAccessKey){
        final BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);
        return basicAWSCredentials;
    }

    @Bean
    @Primary
    public AmazonS3 s3Client(){
        return amazonS3Client(awsCredentialsProvider(), activeBucket);
    }

    public AmazonS3 amazonS3Client(BasicAWSCredentials credentialsProvider, String bucket) {
        final AmazonS3Client amazonS3Client = new AmazonS3Client(credentialsProvider);
        amazonS3Client.getBucketPolicy(bucket);
        return amazonS3Client;
    }

    public CloudStorageItemReference storageItemReference(String contentId){
        return new CloudStorageItemReference(activeBucket, contentId);
    }


    public NodePath returnProcessingStorageTargetFolder(long experiment, String fileName){
        NodePath nodePath = new NodePath(Joiner.on(DELIMETER).join(processedTargetFolder, experiment, fileName));
        return nodePath;
    }

    public NodePath returnExperimentStorageTargetFolder(long user, long instrumentId, String fileName){
        NodePath nodePath = new NodePath(Joiner.on(DELIMETER).join(rawFileTargetFolder, user, instrumentId, fileName));
        return nodePath;
    }


}
