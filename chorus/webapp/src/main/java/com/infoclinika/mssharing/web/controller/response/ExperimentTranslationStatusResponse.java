/*
 * C O P Y R I G H T   N O T I C E
 * -----------------------------------------------------------------------
 * Copyright (c) 2011-2012 InfoClinika, Inc. 5901 152nd Ave SE, Bellevue, WA 98006,
 * United States of America.  (425) 442-8058.  http://www.infoclinika.com.
 * All Rights Reserved.  Reproduction, adaptation, or translation without prior written permission of InfoClinika, Inc. is prohibited.
 * Unpublished--rights reserved under the copyright laws of the United States.  RESTRICTED RIGHTS LEGEND Use, duplication or disclosure by the
 */
package com.infoclinika.mssharing.web.controller.response;

import com.infoclinika.mssharing.model.read.DashboardReader;

import java.util.Date;

/**
 * @author Oleksii Tymchenko
 */
public class ExperimentTranslationStatusResponse {
    public final String msChartsUrl;
    public final String translationErrors;
    public final Date lastTranslationAttemptDate;
    public final boolean proteinIDSearchAllowed;
    public final boolean archived;
    public final DashboardReader.TranslationStatus status;
    public final boolean usedInSearches;

    public ExperimentTranslationStatusResponse(String msChartsUrl, String translationErrors, Date lastTranslationAttemptDate,
                                               boolean proteinIDSearchAllowed, boolean isArchived, DashboardReader.TranslationStatus status,
                                               boolean usedInSearches) {
        this.msChartsUrl = msChartsUrl;
        this.translationErrors = translationErrors;
        this.lastTranslationAttemptDate = lastTranslationAttemptDate;
        this.proteinIDSearchAllowed = proteinIDSearchAllowed;
        this.archived = isArchived;
        this.status = status;
        this.usedInSearches = usedInSearches;
    }
}
