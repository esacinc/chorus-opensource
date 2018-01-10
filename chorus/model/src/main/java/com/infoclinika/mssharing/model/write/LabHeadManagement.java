/*
 * C O P Y R I G H T   N O T I C E
 * -----------------------------------------------------------------------
 * Copyright (c) 2011-2012 InfoClinika, Inc. 5901 152nd Ave SE, Bellevue, WA 98006,
 * United States of America.  (425) 442-8058.  http://www.infoclinika.com.
 * All Rights Reserved.  Reproduction, adaptation, or translation without prior written permission of InfoClinika, Inc. is prohibited.
 * Unpublished--rights reserved under the copyright laws of the United States.  RESTRICTED RIGHTS LEGEND Use, duplication or disclosure by the
 */
package com.infoclinika.mssharing.model.write;

import com.infoclinika.mssharing.platform.model.write.LabHeadManagementTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * Lab head have rights to manipulate with users which are in this laboratory. He can remove user from laboratory.
 * In this case all projects, experiments, files should be re-assigned to lab head from removed user
 *
 * @author andrii.loboda
 */
@Transactional
public interface LabHeadManagement extends LabHeadManagementTemplate {

}