package com.infoclinika.mssharing.model.internal.repository;

/**
 * @author Oleksii Tymchenko
 */
public class TranslatedFileIdInExperiment {

    public final long experimentId;
    public final long fileMetaDataId;

    public TranslatedFileIdInExperiment(long experimentId, long fileMetaDataId) {
        this.experimentId = experimentId;
        this.fileMetaDataId = fileMetaDataId;
    }
}
