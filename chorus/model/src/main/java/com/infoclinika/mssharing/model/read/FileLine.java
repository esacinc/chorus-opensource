package com.infoclinika.mssharing.model.read;

import com.google.common.collect.ImmutableSet;
import com.infoclinika.mssharing.platform.model.read.AccessLevel;
import com.infoclinika.mssharing.platform.model.read.FileReaderTemplate;

import java.util.Collection;
import java.util.Date;

/**
* @author Herman Zamula
*/
public class FileLine extends FileReaderTemplate.FileLineTemplate {
    public final String archiveId;
    public final Date lastPingDate;
    public final boolean isArchive;
    public final DashboardReader.TranslationStatus status;
    public final String translationErrorMessage;
    public final Collection<Long> operators;
    public final boolean translatedSuccessfully;
    public final boolean sentForTranslation;
    public final String msChartsUrl;
    public final DashboardReader.StorageStatus storageStatus;
    public final boolean sizeIsConsistent;

    public final DashboardReader.FileColumns columns;
 
    public final boolean usedInProteinSearches;
    public final ImmutableSet<FileUserFunctionInfoItem> labsWhereTranslated;

    public final boolean toReplace;
    public final String instrumentStudyType;

    public FileLine(long id,
                    String name,
                    long instrumentId,
                    String instrumentName,
                    long modelId,
                    String instrumentModel,
                    long labId,
                    long labHead,
                    Long specieId,
                    String contentId,
                    String archiveId,
                    String uploadId,
                    String destinationPath,
                    boolean archive,
                    AccessLevel accessLevel,
                    String msChartsUrl,
                    boolean usedInExperiments,
                    long owner,
                    Date lastPingDate,
                    DashboardReader.FileColumns columns,
                    boolean invalid,
                    DashboardReader.TranslationStatus status,
                    String translationErrorMessage,
                    String vendorName,
                    Collection<Long> operators,
                    boolean translatedSuccessfully,
                    boolean sentForTranslation,
                    DashboardReader.StorageStatus storageStatus,
                    boolean sizeIsConsistent,
                    boolean usedInProteinSearches,
                    ImmutableSet<FileUserFunctionInfoItem> labsWhereTranslated,
                    boolean toReplace,
                    String instrumentStudyType) {

        super(id, name, contentId, uploadId, destinationPath, instrumentId, labId, instrumentName, modelId, columns.laboratory, owner, labHead, invalid, vendorName, instrumentModel, specieId, accessLevel, usedInExperiments, columns.labels, columns.sizeInBytes, columns.uploadDate);
        this.archiveId = archiveId;
        this.lastPingDate = lastPingDate;
        isArchive = archive;
        this.columns = columns;
        this.status = status;
        this.translationErrorMessage = translationErrorMessage;
        this.operators = operators;
        this.translatedSuccessfully = translatedSuccessfully;
        this.sentForTranslation = sentForTranslation;
        this.msChartsUrl = msChartsUrl;
        this.storageStatus = storageStatus;
        this.sizeIsConsistent = sizeIsConsistent;
        this.usedInProteinSearches = usedInProteinSearches;
        this.labsWhereTranslated = labsWhereTranslated;
        this.toReplace = toReplace;
        this.instrumentStudyType = instrumentStudyType;
    }

    public FileLine(FileReaderTemplate.FileLineTemplate other, String archiveId,
                    Date lastPingDate, boolean isArchive,
                    DashboardReader.TranslationStatus status, String translationErrorMessage, Collection<Long> operators,
                    boolean translatedSuccessfully, boolean sentForTranslation, String msChartsUrl,
                    DashboardReader.StorageStatus storageStatus, boolean sizeIsConsistent,
                    DashboardReader.FileColumns columns, boolean usedInProteinSearches,
                    ImmutableSet<FileUserFunctionInfoItem> labsWhereTranslated, boolean toReplace, String instrumentStudyType) {
        super(other);
        this.archiveId = archiveId;
        this.lastPingDate = lastPingDate;
        this.isArchive = isArchive;
        this.status = status;
        this.translationErrorMessage = translationErrorMessage;
        this.operators = operators;
        this.translatedSuccessfully = translatedSuccessfully;
        this.sentForTranslation = sentForTranslation;
        this.msChartsUrl = msChartsUrl;
        this.storageStatus = storageStatus;
        this.sizeIsConsistent = sizeIsConsistent;
        this.columns = columns;
        this.usedInProteinSearches = usedInProteinSearches;
        this.labsWhereTranslated = labsWhereTranslated;
        this.toReplace = toReplace;
        this.instrumentStudyType = instrumentStudyType;
    }
}
