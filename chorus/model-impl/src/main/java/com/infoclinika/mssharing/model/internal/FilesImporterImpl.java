package com.infoclinika.mssharing.model.internal;

import com.infoclinika.analysis.storage.cloud.CloudStorageItemReference;
import com.infoclinika.mssharing.model.FilesImporter;
import com.infoclinika.mssharing.model.internal.entity.workflow.WorkflowStepEntry;
import com.infoclinika.mssharing.model.internal.repository.WorkflowStepEntryRepository;
import com.infoclinika.mssharing.model.internal.workflow.steps.DownloadFromFtpMessagingClient;
import com.infoclinika.mssharing.model.internal.workflow.steps.MessagingQueueHelper;
import com.infoclinika.mssharing.workflow.steps.WorkflowStepConfiguration.QueueConfiguration;
import com.infoclinika.tasks.api.workflow.input.DownloadFromFtpTask;
import com.infoclinika.tasks.api.workflow.output.DownloadFromFtpTaskResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * @author timofei.kasianov 11/4/16
 */
@Component
public class FilesImporterImpl implements FilesImporter {

    private static final String TOKEN = "downloaderFtp";

    @Value("${amazon.active.bucket}")
    private String targetBucket;

    @Inject
    private DownloadFromFtpMessagingClient downloadFromFtpMessagingClient;
    @Inject
    private WorkflowStepEntryRepository workflowStepEntryRepository;

    @Override
    public List<CloudStorageItemReference> importFromFtp(long actor, FtpImportRequest request) {

        final DownloadFromFtpTask task = new DownloadFromFtpTask(
                request.url,
                request.login,
                request.password,
                targetBucket,
                request.bucketPathPrefix,
                request.masks,
                request.recursive
        );
        final WorkflowStepEntry stepEntry = workflowStepEntryRepository.findOneByAccessKeyToken(TOKEN);
        final QueueConfiguration configuration = MessagingQueueHelper.workflowStepConfigurationDataTransformer(null).apply(stepEntry.getTaskQueueConfiguration());

        final DownloadFromFtpTaskResult taskResult = downloadFromFtpMessagingClient.sendTaskSync(task, configuration);

        if(isNotEmpty(taskResult.errorMessage)) {
            throw new RuntimeException("Unable to import files from FTP: " + request.url + ". Reason: " + taskResult.errorMessage);
        }

        return taskResult.downloadedFiles;
    }


}
