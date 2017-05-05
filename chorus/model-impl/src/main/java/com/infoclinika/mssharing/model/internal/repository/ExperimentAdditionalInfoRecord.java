package com.infoclinika.mssharing.model.internal.repository;

/**
 * @author andrii.loboda
 */
public class ExperimentAdditionalInfoRecord {
    public final long experiment;
    public final long userCanCreateExperimentsInProject;
    public final long countFilesReadyToDownload;
    public final long countFilesWhichCouldBeTranslated;
    public final long canArchiveExperiment;
    public final long canUnarchiveExperiment;
    public final long countFilesTranslatedForOwnerSuccessfully;//TODO:2015-05-25:andrii.loboda: it seems to not work - please check it until 23 August 2015
    public final long countFilesTranslatedByUser;
    public final long countArchivedFilesRequestedForDownloadOnly;
    public final long countArchivedFilesRequestedForUnArchiving;
    public final long countFilesTranslationForOwnerInProgress;
    public final long countFilesTranslationForOwnerFailed;
    public final long countFilesTranslationUserSpecificSuccess;
    public final long countFilesTranslationUserSpecificInProgress;
    public final long countFilesTranslationUserSpecificFailed;
    public final long countFilesTranslatedSuccesfullyTotal;

    public ExperimentAdditionalInfoRecord(long experiment, long userCanCreateExperimentsInProject, long countFilesReadyToDownload, long countFilesWhichCouldBeTranslated,
                                          long canArchiveExperiment, long canUnarchiveExperiment, long countFilesTranslatedForOwnerSuccessfully, long countFilesTranslatedByUser,
                                          long countArchivedFilesRequestedForDownloadOnly, long countArchivedFilesRequestedForUnArchiving, long countFilesTranslationForOwnerInProgress,long countFilesTranslationForOwnerFailed,
                                          long countFilesTranslationUserSpecificSuccess, long countFilesTranslationUserSpecificInProgress, long countFilesTranslationUserSpecificFailed,
                                          long countFilesTranslatedSuccesfullyTotal) {
        this.experiment = experiment;
        this.userCanCreateExperimentsInProject = userCanCreateExperimentsInProject;
        this.countFilesReadyToDownload = countFilesReadyToDownload;
        this.countFilesWhichCouldBeTranslated = countFilesWhichCouldBeTranslated;
        this.canArchiveExperiment = canArchiveExperiment;
        this.canUnarchiveExperiment = canUnarchiveExperiment;
        this.countFilesTranslatedForOwnerSuccessfully = countFilesTranslatedForOwnerSuccessfully;
        this.countFilesTranslatedByUser = countFilesTranslatedByUser;
        this.countArchivedFilesRequestedForDownloadOnly = countArchivedFilesRequestedForDownloadOnly;
        this.countArchivedFilesRequestedForUnArchiving = countArchivedFilesRequestedForUnArchiving;
        this.countFilesTranslationForOwnerInProgress = countFilesTranslationForOwnerInProgress;
        this.countFilesTranslationForOwnerFailed = countFilesTranslationForOwnerFailed;
        this.countFilesTranslationUserSpecificSuccess = countFilesTranslationUserSpecificSuccess;
        this.countFilesTranslationUserSpecificInProgress = countFilesTranslationUserSpecificInProgress;
        this.countFilesTranslationUserSpecificFailed = countFilesTranslationUserSpecificFailed;
        this.countFilesTranslatedSuccesfullyTotal = countFilesTranslatedSuccesfullyTotal;
    }
}
