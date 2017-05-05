package com.infoclinika.mssharing.model.write;

import com.infoclinika.mssharing.platform.model.write.FileManagementTemplate;

/**
 * @author Herman Zamula
 */
public class FileMetaDataInfo extends FileManagementTemplate.FileMetaDataInfoTemplate {

    public final boolean autotranslate;

    public FileMetaDataInfo(String fileName, long sizeInBytes, String labels, String destinationPath, long species, boolean archive, boolean autotranslate) {
        super(fileName, sizeInBytes, labels, destinationPath, species, archive);
        this.autotranslate = autotranslate;
    }
}
