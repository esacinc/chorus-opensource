/*
 * C O P Y R I G H T   N O T I C E
 * -----------------------------------------------------------------------
 * Copyright (c) 2011-2012 InfoClinika, Inc. 5901 152nd Ave SE, Bellevue, WA 98006,
 * United States of America.  (425) 442-8058.  http://www.infoclinika.com.
 * All Rights Reserved.  Reproduction, adaptation, or translation without prior written permission of InfoClinika, Inc. is prohibited.
 * Unpublished--rights reserved under the copyright laws of the United States.  RESTRICTED RIGHTS LEGEND Use, duplication or disclosure by the
 */
package com.infoclinika.mssharing.model.internal.repository;

import com.infoclinika.mssharing.model.internal.entity.workflow.WorkflowStepEntry;
import com.infoclinika.mssharing.model.internal.entity.workflow.WorkflowStepTypeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


/**
 * @author andri.loboda
 */
public interface WorkflowStepEntryRepository extends JpaRepository<WorkflowStepEntry, Long> {
    @Query("select e from WorkflowStepEntry e where e.accessKeyToken=:accessKeyToken and e.accessSecretToken = :accessSecretToken")
    WorkflowStepEntry findOneByTokens(@Param("accessKeyToken") String accessKeyToken, @Param("accessSecretToken") String accessSecretToken);

    @Query("select e from WorkflowStepEntry e where e.accessKeyToken=:accessKeyToken")
    WorkflowStepEntry findOneByAccessKeyToken(@Param("accessKeyToken") String accessKeyToken);

    @Query("select e from WorkflowStepEntry e where e.id in (:workflowSteps)")
    List<WorkflowStepEntry> findByIds(@Param("workflowSteps") List<Long> workflowSteps);

    @Query("select e from WorkflowStepEntry e where e.type=:stepType")
    List<WorkflowStepEntry> findByType(@Param("stepType") WorkflowStepTypeEntry stepType);
    @Query("select e from WorkflowStepEntry e where e.processorClassLocation in (:processorClassPaths)")
    List<WorkflowStepEntry> findByProcessorClassPath(@Param("processorClassPaths") List<String> processorClassPaths);
}
