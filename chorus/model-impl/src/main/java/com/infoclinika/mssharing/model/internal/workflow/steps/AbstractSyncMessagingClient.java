package com.infoclinika.mssharing.model.internal.workflow.steps;

import com.infoclinika.mssharing.workflow.steps.WorkflowStepConfiguration;
import com.infoclinika.tasks.api.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JsonMessageConverter;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Bogdan Kovalev
 *         Created on 11/24/16.
 */
abstract class AbstractSyncMessagingClient<TaskResultClass> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadFromFtpMessagingClient.class);

    @Inject
    private JsonMessageConverter messageConverter;

    @SuppressWarnings("unchecked")
    public TaskResultClass sendTaskSync(AbstractTask task, WorkflowStepConfiguration.QueueConfiguration configuration) {
        checkNotNull(configuration, "Queue configuration not specified.");

        initializeQueue(configuration);
        final RabbitTemplate rabbitTemplate = MessagingQueueHelper.getRabbitTemplate(configuration, messageConverter);

        LOGGER.debug("Sending synchronously a task = " + task + ". Type of: " + task.getClass().getName());
        return (TaskResultClass) rabbitTemplate.convertSendAndReceive(configuration.queue, prepareMessageForSync(task));
    }

    private Message prepareMessageForSync(AbstractTask task) {
        final MessageProperties messageProperties = new MessageProperties();
        return messageConverter.toMessage(task, messageProperties);
    }

    private void initializeQueue(WorkflowStepConfiguration.QueueConfiguration configuration) {
        MessagingQueueHelper.declareQueueIfNotDeclared(
                MessagingQueueHelper.getRabbitTemplate(configuration, messageConverter),
                configuration.queue
        );
    }
}
