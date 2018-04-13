package com.infoclinika.mssharing.web.controller.v2.service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.collect.ImmutableList;
import com.infoclinika.analysis.storage.cloud.CloudStorageFactory;
import com.infoclinika.analysis.storage.cloud.CloudStorageItemReference;
import com.infoclinika.analysis.storage.cloud.CloudStorageService;
import com.infoclinika.mssharing.model.helper.ExperimentCreationHelper;
import com.infoclinika.mssharing.model.read.DashboardReader;
import com.infoclinika.mssharing.model.read.DetailsReader;
import com.infoclinika.mssharing.model.read.ExtendedShortExperimentFileItem;
import com.infoclinika.mssharing.model.read.dto.details.ExperimentItem;
import com.infoclinika.mssharing.platform.fileserver.model.NodePath;
import com.infoclinika.mssharing.platform.model.common.items.DictionaryItem;
import com.infoclinika.mssharing.platform.model.read.DetailsReaderTemplate;
import com.infoclinika.mssharing.platform.model.read.InstrumentModelReaderTemplate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.net.URL;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ExperimentService {

    private static final Logger LOGGER = Logger.getLogger(ExperimentService.class);

    @Inject
    private AmazonS3 s3Client;

    @Inject
    private AwsConfigService awsConfigService;

    @Inject
    private DetailsReader detailsReader;

    @Inject
    private DashboardReader dashboardReader;

    @Inject
    private ExperimentCreationHelper experimentCreationHelper;

    private static final CloudStorageService CLOUD_STORAGE_SERVICE = CloudStorageFactory.service();



    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExperimentDetails {
        private ExperimentItem experimentItem;
        private DetailsReaderTemplate.ExperimentShortInfo experimentShortInfo;
    }

    @Data
    @JsonPropertyOrder(alphabetic = true)
    public static class ExperimentInfoDTO {
        private String name;
        private String labName;
        private Long projectId;
        private String projectName;
        private Long laboratory;
        private String vendor;
        private String technologyType;
        private String description;
        private String species;
        private String instrumentModel;
        private String instrument;
        private Long experimentType;
        private Map<String, Collection<FileToSamplesDTO>> filesToSamples;
    }

    @Data
    public static class FileToSamplesDTO{
        private String filePath;
        private String sampleName;
    }



    public ExperimentInfoDTO returnExperimentInfo(long userId, long experimentId){
        return toExperimentInfoDTO(new ExperimentDetails(detailsReader.readExperiment(userId, experimentId), detailsReader.readExperimentShortInfo(userId, experimentId)));
    }

    private ExperimentInfoDTO experimentDetailsToInfoDTO(ExperimentDetails experimentDetails, ExperimentInfoDTO destination){

        ExperimentItem experimentItemSource = experimentDetails.getExperimentItem();
        DictionaryItem dictionaryItem = experimentCreationHelper.specie(experimentItemSource.specie);
        DetailsReaderTemplate.ExperimentShortInfo shortInfo = experimentDetails.getExperimentShortInfo();
        InstrumentModelReaderTemplate.InstrumentModelLineTemplate instrumentModel = dashboardReader.readById(experimentItemSource.labHead, experimentItemSource.instrumentModel);

        destination.setName(experimentItemSource.name);
        destination.setLabName(shortInfo.labName);
        destination.setLaboratory(experimentItemSource.lab);
        destination.setDescription(experimentItemSource.description);
        destination.setInstrument(experimentItemSource.instrumentName);
        destination.setInstrumentModel(instrumentModel.name);
        destination.setVendor(experimentItemSource.instrumentVendor);
        destination.setLabName(experimentItemSource.labName);
        destination.setProjectId(experimentItemSource.project);
        destination.setProjectName(shortInfo.projectName);
        destination.setSpecies(dictionaryItem.name);
        destination.setTechnologyType(instrumentModel.technologyType.name);
        destination.setExperimentType(experimentItemSource.experimentType);

        destination.setFilesToSamples(computeExperimentFileSamples(shortInfo.files, experimentItemSource.labHead, experimentItemSource.instrument.get()));


        return destination;
    }

    private Map<String, Collection<FileToSamplesDTO>> computeExperimentFileSamples(List<? extends DetailsReaderTemplate.ShortExperimentFileItem> files, long user, long instrumentId){

        Map<String, Collection<FileToSamplesDTO>> map = new HashMap<>();

        for(DetailsReaderTemplate.ShortExperimentFileItem file : files){

            ExtendedShortExperimentFileItem fileItem = (ExtendedShortExperimentFileItem) file;
            List<FileToSamplesDTO> list = new ArrayList<>();

            if(!map.containsKey(file.name)){

                ImmutableList<ExtendedShortExperimentFileItem.ExperimentShortSampleItem> immutableList = fileItem.samples;
                FileToSamplesDTO fileToSamplesDTO = new FileToSamplesDTO();

                for (ExtendedShortExperimentFileItem.ExperimentShortSampleItem sampleItem : immutableList){
                    fileToSamplesDTO.setSampleName(sampleItem.condition.name);
                }

                final NodePath nodePath = awsConfigService.returnStorageTargetFolder(user, instrumentId, file.name);

                fileToSamplesDTO.setFilePath(generateTemporaryLinkToS3(nodePath.getPath()));
                list.add(fileToSamplesDTO);
                map.put(file.name, list);
            }
        }

        return map;
    }

    private ExperimentInfoDTO toExperimentInfoDTO(ExperimentDetails experimentItemSource){
        return experimentDetailsToInfoDTO(experimentItemSource, new ExperimentInfoDTO());
    }



    private String generateTemporaryLinkToS3(String key){

        String bucket = awsConfigService.getActiveBucket();
        CloudStorageItemReference cloudStorageItemReference = awsConfigService.storageItemReference(key);

        try {

            if(CLOUD_STORAGE_SERVICE.existsAtCloud(cloudStorageItemReference)){

                Date expiration = new Date();
                long milliSeconds = expiration.getTime();
                milliSeconds += 1000 * 60 * 60;
                expiration.setTime(milliSeconds);

                GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucket, key);
                generatePresignedUrlRequest.setMethod(HttpMethod.GET);
                generatePresignedUrlRequest.setExpiration(expiration);

                URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);

                return url.toString();
            }

        } catch (AmazonServiceException exception) {
            LOGGER.warn("Caught an AmazonServiceException, " + "which means your request made it " + "to Amazon S3, but was rejected with an error response " + "for some reason.");
            LOGGER.warn("Error Message: " + exception.getMessage());
            LOGGER.warn("HTTP  Code: "    + exception.getStatusCode());
            LOGGER.warn("AWS Error Code:" + exception.getErrorCode());
            LOGGER.warn("Error Type:    " + exception.getErrorType());
            LOGGER.warn("Request ID:    " + exception.getRequestId());
        } catch (AmazonClientException ace) {
            LOGGER.warn("Caught an AmazonClientException, " + "which means the client encountered " + "an internal error while trying to communicate" + " with S3, " +
                    "such as not being able to access the network.");
            LOGGER.warn("Error Message: " + ace.getMessage());
        }

        return "file path does not exists";
    }
}
