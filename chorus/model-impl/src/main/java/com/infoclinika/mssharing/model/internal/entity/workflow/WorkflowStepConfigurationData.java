package com.infoclinika.mssharing.model.internal.entity.workflow;

import com.infoclinika.mssharing.model.internal.entity.AbstractAggregate;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author andrii.loboda on 27.08.2014.
 *         <p/>
 *         Contains necessary information about queue, its credentials and where this queue is located
 */
@Entity
@Table(name = "w_WorkflowStepConfiguration")
public class WorkflowStepConfigurationData extends AbstractAggregate {
    @Basic(optional = false)
    private String queueName;
    private String replyQueueName; // could be null
    @Basic(optional = false)
    private int port;
    @Basic(optional = false)
    private String hostName;
    @Basic(optional = false)
    private String username;
    @Basic(optional = false)
    private String password;
    @Basic(optional = false)
    private int timeout;

    public WorkflowStepConfigurationData() {
    }

    public WorkflowStepConfigurationData(String queueName,String replyQueueName, int port, String hostName, String username, String password, int timeout) {
        this.queueName = queueName;
        this.replyQueueName = replyQueueName;
        this.port = port;
        this.hostName = hostName;
        this.username = username;
        this.password = password;
        this.timeout = timeout;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getReplyQueueName() {
        return replyQueueName;
    }

    public void setReplyQueueName(String replyQueueName) {
        this.replyQueueName = replyQueueName;
    }
}
