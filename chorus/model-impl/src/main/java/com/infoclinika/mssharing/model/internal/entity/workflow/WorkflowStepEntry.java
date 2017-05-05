/*
 * C O P Y R I G H T   N O T I C E
 * -----------------------------------------------------------------------
 * Copyright (c) 2011-2012 InfoClinika, Inc. 5901 152nd Ave SE, Bellevue, WA 98006,
 * United States of America.  (425) 442-8058.  http://www.infoclinika.com.
 * All Rights Reserved.  Reproduction, adaptation, or translation without prior written permission of InfoClinika, Inc. is prohibited.
 * Unpublished--rights reserved under the copyright laws of the United States.  RESTRICTED RIGHTS LEGEND Use, duplication or disclosure by the
 */
package com.infoclinika.mssharing.model.internal.entity.workflow;

import com.infoclinika.mssharing.model.internal.entity.AbstractAggregate;

import javax.persistence.*;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

/**
 * @author andrii.loboda on 27.08.2014.
 *
 * Represents specific implementation of workflow step wiht its configuration
 */
@Entity
@Table(name = "w_WorkflowStep")
public class WorkflowStepEntry extends AbstractAggregate {
    @Column(length = 1000)
    @Basic(optional = false)
    private String name;
    @Column(length = 4000)
    private String description;
    @OneToOne(cascade = CascadeType.ALL, optional = false)
    private WorkflowStepConfigurationData taskQueueConfiguration;
    @OneToOne(cascade = CascadeType.ALL)
    private WorkflowStepConfigurationData taskRemoveConfiguration;
    @OneToOne(cascade = CascadeType.ALL)
    private WorkflowStepConfigurationData taskEstimateConfiguration;

    @ManyToOne(optional = false)
    @JoinColumns({@JoinColumn(name = "type_id")})
    private WorkflowStepTypeEntry type;

    @Basic(optional = false)
    private String accessKeyToken;
    @Basic(optional = false)
    private String accessSecretToken;

    @Lob
    private String userInterfaceDescription;

    @Basic(optional = false)
    @Column(length = 300)
    private String processorClassLocation;

    @Basic(optional = false)
    @Column(length = 300)
    private String taskClassLocation;

    @Basic(optional = false)
    @Column(length = 300)
    private String taskResultClassLocation;

    @OneToMany
    @JoinTable(
            name="w_TypesToSkip",
            joinColumns = @JoinColumn( name="step_id"),
            inverseJoinColumns = @JoinColumn( name="step_type_id")
    )
    private Set<WorkflowStepTypeEntry> typesToSkip = newHashSet();

    @Basic(optional = false)
    private boolean disabled = false;

    public WorkflowStepEntry() {
    }

    public WorkflowStepEntry(String name, String description, WorkflowStepTypeEntry type, String accessKeyToken, String accessSecretToken,
                             WorkflowStepConfigurationData taskQueueConfiguration, WorkflowStepConfigurationData taskRemoveConfiguration, WorkflowStepConfigurationData taskEstimateConfiguration,
                             String processorClassLocation, String taskClassLocation, String taskResultClassLocation,
                             String userInterfaceDescription) {
        this.name = name;
        this.description = description;
        this.accessKeyToken = accessKeyToken;
        this.accessSecretToken = accessSecretToken;
        this.taskQueueConfiguration = taskQueueConfiguration;
        this.taskRemoveConfiguration = taskRemoveConfiguration;
        this.taskEstimateConfiguration = taskEstimateConfiguration;
        this.processorClassLocation = processorClassLocation;
        this.taskClassLocation = taskClassLocation;
        this.taskResultClassLocation = taskResultClassLocation;
        this.userInterfaceDescription = userInterfaceDescription;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WorkflowStepTypeEntry getType() {
        return type;
    }

    public void setType(WorkflowStepTypeEntry type) {
        this.type = type;
    }

    public WorkflowStepConfigurationData getTaskQueueConfiguration() {
        return taskQueueConfiguration;
    }

    public void setTaskQueueConfiguration(WorkflowStepConfigurationData taskQueueConfiguration) {
        this.taskQueueConfiguration = taskQueueConfiguration;
    }

    public WorkflowStepConfigurationData getTaskRemoveConfiguration() {
        return taskRemoveConfiguration;
    }

    public void setTaskRemoveConfiguration(WorkflowStepConfigurationData taskRemoveConfiguration) {
        this.taskRemoveConfiguration = taskRemoveConfiguration;
    }

    public WorkflowStepConfigurationData getTaskEstimateConfiguration() {
        return taskEstimateConfiguration;
    }

    public void setTaskEstimateConfiguration(WorkflowStepConfigurationData taskEstimateConfiguration) {
        this.taskEstimateConfiguration = taskEstimateConfiguration;
    }

    public String getAccessKeyToken() {
        return accessKeyToken;
    }

    public void setAccessKeyToken(String accessKeyToken) {
        this.accessKeyToken = accessKeyToken;
    }

    public String getAccessSecretToken() {
        return accessSecretToken;
    }

    public void setAccessSecretToken(String accessSecretToken) {
        this.accessSecretToken = accessSecretToken;
    }

    public String getUserInterfaceDescription() {
        return userInterfaceDescription;
    }

    public void setUserInterfaceDescription(String userInterfaceDescription) {
        this.userInterfaceDescription = userInterfaceDescription;
    }

    public String getTaskClassLocation() {
        return taskClassLocation;
    }

    public void setTaskClassLocation(String taskClassLocation) {
        this.taskClassLocation = taskClassLocation;
    }

    public String getTaskResultClassLocation() {
        return taskResultClassLocation;
    }

    public void setTaskResultClassLocation(String taskResultClassLocation) {
        this.taskResultClassLocation = taskResultClassLocation;
    }

    public Set<WorkflowStepTypeEntry> getTypesToSkip() {
        return typesToSkip;
    }

    public void setTypesToSkip(Set<WorkflowStepTypeEntry> typesToSkip) {
        this.typesToSkip = typesToSkip;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String getProcessorClassLocation() {
        return processorClassLocation;
    }

    public void setProcessorClassLocation(String processorClassLocation) {
        this.processorClassLocation = processorClassLocation;
    }
}
