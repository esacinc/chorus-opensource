package com.infoclinika.mssharing.model.internal.workflow.steps;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.infoclinika.mssharing.model.internal.entity.workflow.WorkflowStepConfigurationData;
import com.infoclinika.tasks.api.ConnectionSettings;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;

import javax.annotation.Nullable;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.infoclinika.mssharing.workflow.steps.WorkflowStepConfiguration.QueueConfiguration;

/**
 * @author Vladislav Kovchug
 */
public class MessagingQueueHelper {

    private static final Logger LOG = LoggerFactory.getLogger(MessagingQueueHelper.class);

    public static ConnectionFactory composeConnectionFactory(QueueConfiguration configuration) {
        final CachingConnectionFactory connectionFactory = new CachingConnectionFactory(configuration.hostName, configuration.port);
        connectionFactory.setUsername(configuration.username);
        connectionFactory.setPassword(configuration.password);
        return connectionFactory;
    }

    public static RabbitTemplate getRabbitTemplate(QueueConfiguration configuration, MessageConverter messageConverter) {
        final ConnectionFactory connectionFactory = composeConnectionFactory(configuration);

        final RabbitTemplate rabbitTemplate = new RabbitTemplate();
        rabbitTemplate.setConnectionFactory(connectionFactory);
        rabbitTemplate.setQueue(configuration.queue);
        rabbitTemplate.setMessageConverter(messageConverter);
        rabbitTemplate.setReplyTimeout(configuration.timeout);
        return rabbitTemplate;
    }

    public static RabbitTemplate getRabbitTemplateWithFixedReplyQueue(QueueConfiguration configuration, MessageConverter messageConverter) {
        checkNotNull(configuration.replyQueue, "Reply Queue should be specified in order to get RabbitTemplate for async");
        final RabbitTemplate rabbitTemplate = getRabbitTemplate(configuration, messageConverter);
        rabbitTemplate.setReplyQueue(new Queue(configuration.replyQueue));
        return rabbitTemplate;
    }

    public static int getQueueConsumersCount(RabbitTemplate rabbitTemplate, String replyQueue) {
        Channel channel = null;
        try {
            final ConnectionFactory connectionFactory = rabbitTemplate.getConnectionFactory();
            final Connection connection = connectionFactory.createConnection();
            channel = connection.createChannel(true);
            final AMQP.Queue.DeclareOk result = channel.queueDeclarePassive(replyQueue);

            return result.getConsumerCount();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                    LOG.error("Ignored closing channel for rabbit: " + e.getMessage(), e);
                }
            }

        }
    }

    public static void declareQueueIfNotDeclared(RabbitTemplate rabbitTemplate, String queueToDeclare) {
        Channel channel = null;
        try {
            final ConnectionFactory connectionFactory = rabbitTemplate.getConnectionFactory();
            final Connection connection = connectionFactory.createConnection();
            channel = connection.createChannel(true);
            final AMQP.Queue.DeclareOk result = channel.queueDeclare(queueToDeclare, true, false, false, Maps.newHashMap());
            channel.close();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                    LOG.error("Ignored closing channel for rabbit: " + e.getMessage(), e);
                }
            }

        }
    }

    public static ConnectionSettings composeConnectionSettings(String queueName, QueueConfiguration configuration) {
        return new ConnectionSettings(configuration.hostName, Integer.toString(configuration.port), configuration.username, configuration.password, queueName);
    }

    public static Function<WorkflowStepConfigurationData, QueueConfiguration> workflowStepConfigurationDataTransformer(final String replyQueueSuffix) {
        return new Function<WorkflowStepConfigurationData, QueueConfiguration>() {
            @Nullable
            @Override
            public QueueConfiguration apply(@Nullable WorkflowStepConfigurationData input) {

                if (input == null) {
                    return null;
                }
                String replyQueueName = input.getReplyQueueName();
                if (replyQueueName != null && replyQueueSuffix != null) { // to avoid task stealing from production queue for development
                    replyQueueName = replyQueueName + replyQueueSuffix;
                }
                return new QueueConfiguration(input.getHostName(), input.getPort(), input.getUsername(), input.getPassword(), input.getQueueName(), replyQueueName, input.getTimeout());
            }
        };
    }

}
