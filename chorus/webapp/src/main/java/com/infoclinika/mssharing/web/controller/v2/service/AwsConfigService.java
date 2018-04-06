package com.infoclinika.mssharing.web.controller.v2.service;


import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.net.URL;

@Configuration
@Service
public class AwsConfigService {

    private static final Logger LOGGER = Logger.getLogger(AwsConfigService.class);



    @Value("${raw.files.target.folder}")
    private String targetFolder;

    @Value("${amazon.key}")
    private String accessKeyId;

    @Value("${amazon.secret}")
    private String secretAccessKey;

    @Value("${amazon.active.bucket}")
    private String activeBucket;

    public String getActiveBucket() {
        return activeBucket;
    }

    public String getTargetFolder(){
        return targetFolder;
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
        return s3Client(awsCredentialsProvider(), activeBucket);
    }

    public AmazonS3 s3Client(BasicAWSCredentials credentialsProvider, String bucket) {
        final AmazonS3Client amazonS3Client = new AmazonS3Client(credentialsProvider);
        amazonS3Client.getBucketPolicy(bucket);
        return amazonS3Client;

    }


}
