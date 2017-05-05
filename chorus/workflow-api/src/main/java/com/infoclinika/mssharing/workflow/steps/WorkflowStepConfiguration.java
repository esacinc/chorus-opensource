package com.infoclinika.mssharing.workflow.steps;

import javax.annotation.Nullable;

/**
 * Contains common information about queue names, queue address, workflow step type id, maybe persister class, description
 */
public class WorkflowStepConfiguration {
    public final QueueConfiguration taskQueueConfiguration;
    public final QueueConfiguration taskRemoveConfiguration;
    public final QueueConfiguration taskEstimateConfiguration;

    public WorkflowStepConfiguration(QueueConfiguration taskQueueConfiguration,
                                     QueueConfiguration taskRemoveConfiguration, QueueConfiguration taskEstimateConfiguration) {
        this.taskQueueConfiguration = taskQueueConfiguration;
        this.taskRemoveConfiguration = taskRemoveConfiguration;
        this.taskEstimateConfiguration = taskEstimateConfiguration;
    }

    @Override
    public String toString() {
        return "WorkflowStepConfiguration{" +
                "taskQueueConfiguration=" + taskQueueConfiguration +
                ", taskRemoveConfiguration=" + taskRemoveConfiguration +
                ", taskEstimateConfiguration=" + taskEstimateConfiguration +
                '}';
    }

    public static class QueueConfiguration{
        public final String hostName;
        public final int port;
        public final String username;
        public final String password;
        public final String queue;
        public final @Nullable String replyQueue;
        public final int timeout;

        public QueueConfiguration(String hostName, int port, String username, String password, String queue, @Nullable String replyQueue, int timeout) {
            this.hostName = hostName;
            this.port = port;
            this.username = username;
            this.password = password;
            this.queue = queue;
            this.replyQueue = replyQueue;
            this.timeout = timeout;
        }

        @Override
        public String toString() {
            return "QueueConfiguration{" +
                    "hostName='" + hostName + '\'' +
                    ", port=" + port +
                    ", username='" + username + '\'' +
                    ", password='" + password + '\'' +
                    ", queue='" + queue + '\'' +
                    ", replyQueue='" + replyQueue + '\'' +
                    ", timeout=" + timeout +
                    '}';
        }
    }
}
