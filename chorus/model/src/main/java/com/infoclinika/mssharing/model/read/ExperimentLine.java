package com.infoclinika.mssharing.model.read;

import com.infoclinika.mssharing.platform.model.read.AccessLevel;
import com.infoclinika.mssharing.platform.model.read.ExperimentReaderTemplate;
import com.infoclinika.mssharing.platform.model.read.LabReaderTemplate;

import java.util.Date;

/**
 * @author Herman Zamula
 */
public class ExperimentLine extends ExperimentReaderTemplate.ExperimentLineTemplate {

    public final boolean isOwner;
    public final String msChartsUrl;
    public final boolean translated;
    public final String translationErrors;
    public final Date lastTranslationAttemptDate;
    public final boolean isAvailableForCopying;
    public final boolean downloadAvailable;
    public final boolean hasUnArchiveRequest;
    public final boolean hasUnArchiveDownloadOnlyRequest;
    public final boolean translationAvailable;
    public final boolean canUnarchive;
    public final boolean canArchive;
    public final boolean proteinSearchEnabled;
    public final String proteinSearchEnabledErrorMessage;
    public final int analyzesCount;
    public final Long billLab;
    public final long countOfOwnedFilesTranslationData;
    public final DashboardReader.TranslationStatus translationStatus;
    public final DashboardReader.ExperimentColumns columns;

    public ExperimentLine(long id, LabReaderTemplate.LabLineTemplate lab, String name, String creator, String project,
                          long files, Date modified, AccessLevel accessLevel, String msChartsUrl,
                          boolean translated, String translationErrors, Date lastTranslationAttemptDate,
                          String downloadLink,
                          boolean owner, boolean isAvailableForCopying, boolean downloadAvailable, boolean hasUnArchiveRequest, boolean hasUnArchiveDownloadOnlyRequest,
                          boolean translationAvailable, boolean canArchive, boolean canUnarchive,
                          int analyzesCount, boolean proteinSearchEnabled, String proteinSearchEnabledErrorMessage,
                          Long billLab, long countOfOwnedFilesTranslationData, long ownerId, DashboardReader.TranslationStatus translationStatus, DashboardReader.ExperimentColumns columns) {
        super(id, name, project, files, modified, lab, downloadLink, creator, accessLevel, ownerId);
        this.msChartsUrl = msChartsUrl;
        this.translated = translated;
        this.translationErrors = translationErrors;
        this.lastTranslationAttemptDate = lastTranslationAttemptDate;
        isOwner = owner;
        this.isAvailableForCopying = isAvailableForCopying;
        this.downloadAvailable = downloadAvailable;
        this.hasUnArchiveRequest = hasUnArchiveRequest;
        this.hasUnArchiveDownloadOnlyRequest = hasUnArchiveDownloadOnlyRequest;
        this.translationAvailable = translationAvailable;
        this.canArchive = canArchive;
        this.canUnarchive = canUnarchive;
        this.proteinSearchEnabled = proteinSearchEnabled;
        this.proteinSearchEnabledErrorMessage = proteinSearchEnabledErrorMessage;
        this.analyzesCount = analyzesCount;
        this.billLab = billLab;
        this.countOfOwnedFilesTranslationData = countOfOwnedFilesTranslationData;
        this.translationStatus = translationStatus;
        this.columns = columns;
    }

    public ExperimentLine(ExperimentReaderTemplate.ExperimentLineTemplate other,
                          boolean isOwner, String msChartsUrl, boolean translated, String translationErrors,
                          Date lastTranslationAttemptDate, boolean isAvailableForCopying, boolean downloadAvailable, boolean hasUnArchiveRequest, boolean hasUnArchiveDownloadOnlyRequest,
                          boolean translationAvailable, boolean canUnarchive, boolean canArchive,
                          boolean proteinSearchEnabled, String proteinSearchEnabledErrorMessage,
                          int analyzesCount, Long billLab, long countOfOwnedFilesTranslationData, DashboardReader.TranslationStatus translationStatus, DashboardReader.ExperimentColumns columns) {
        super(other);
        this.isOwner = isOwner;
        this.msChartsUrl = msChartsUrl;
        this.translated = translated;
        this.translationErrors = translationErrors;
        this.lastTranslationAttemptDate = lastTranslationAttemptDate;
        this.isAvailableForCopying = isAvailableForCopying;
        this.downloadAvailable = downloadAvailable;
        this.hasUnArchiveRequest = hasUnArchiveRequest;
        this.hasUnArchiveDownloadOnlyRequest = hasUnArchiveDownloadOnlyRequest;
        this.translationAvailable = translationAvailable;
        this.canUnarchive = canUnarchive;
        this.canArchive = canArchive;
        this.proteinSearchEnabled = proteinSearchEnabled;
        this.proteinSearchEnabledErrorMessage = proteinSearchEnabledErrorMessage;
        this.analyzesCount = analyzesCount;
        this.billLab = billLab;
        this.countOfOwnedFilesTranslationData = countOfOwnedFilesTranslationData;
        this.translationStatus = translationStatus;
        this.columns = columns;
    }

    public ExperimentLine(ExperimentReaderTemplate.ExperimentLineTemplate lineTemplate, long billLab) {
        this(lineTemplate, false, null, false, null, null, false, false, false, false, false, false, false, false, null,
                0, billLab, 0, DashboardReader.TranslationStatus.NOT_STARTED, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExperimentLine)) return false;
        if (!super.equals(o)) return false;

        ExperimentLine that = (ExperimentLine) o;

        if (analyzesCount != that.analyzesCount) return false;
        if (canArchive != that.canArchive) return false;
        if (canUnarchive != that.canUnarchive) return false;
        if (countOfOwnedFilesTranslationData != that.countOfOwnedFilesTranslationData) return false;
        if (downloadAvailable != that.downloadAvailable) return false;
        if (hasUnArchiveDownloadOnlyRequest != that.hasUnArchiveDownloadOnlyRequest) return false;
        if (hasUnArchiveRequest != that.hasUnArchiveRequest) return false;
        if (isAvailableForCopying != that.isAvailableForCopying) return false;
        if (isOwner != that.isOwner) return false;
        if (proteinSearchEnabled != that.proteinSearchEnabled) return false;
        if (translated != that.translated) return false;
        if (translationAvailable != that.translationAvailable) return false;
        if (billLab != null ? !billLab.equals(that.billLab) : that.billLab != null) return false;
        if (columns != null ? !columns.equals(that.columns) : that.columns != null) return false;
        if (lastTranslationAttemptDate != null ? !lastTranslationAttemptDate.equals(that.lastTranslationAttemptDate) : that.lastTranslationAttemptDate != null)
            return false;
        if (msChartsUrl != null ? !msChartsUrl.equals(that.msChartsUrl) : that.msChartsUrl != null) return false;
        if (translationErrors != null ? !translationErrors.equals(that.translationErrors) : that.translationErrors != null)
            return false;
        if (translationStatus != that.translationStatus) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (isOwner ? 1 : 0);
        result = 31 * result + (msChartsUrl != null ? msChartsUrl.hashCode() : 0);
        result = 31 * result + (translated ? 1 : 0);
        result = 31 * result + (translationErrors != null ? translationErrors.hashCode() : 0);
        result = 31 * result + (lastTranslationAttemptDate != null ? lastTranslationAttemptDate.hashCode() : 0);
        result = 31 * result + (isAvailableForCopying ? 1 : 0);
        result = 31 * result + (downloadAvailable ? 1 : 0);
        result = 31 * result + (hasUnArchiveRequest ? 1 : 0);
        result = 31 * result + (hasUnArchiveDownloadOnlyRequest ? 1 : 0);
        result = 31 * result + (translationAvailable ? 1 : 0);
        result = 31 * result + (canUnarchive ? 1 : 0);
        result = 31 * result + (canArchive ? 1 : 0);
        result = 31 * result + (proteinSearchEnabled ? 1 : 0);
        result = 31 * result + analyzesCount;
        result = 31 * result + (billLab != null ? billLab.hashCode() : 0);
        result = 31 * result + (int) (countOfOwnedFilesTranslationData ^ (countOfOwnedFilesTranslationData >>> 32));
        result = 31 * result + (translationStatus != null ? translationStatus.hashCode() : 0);
        result = 31 * result + (columns != null ? columns.hashCode() : 0);
        return result;
    }
}
