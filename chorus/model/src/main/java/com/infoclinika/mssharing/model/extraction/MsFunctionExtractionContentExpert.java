package com.infoclinika.mssharing.model.extraction;

import com.infoclinika.analysis.storage.cloud.CloudStorageItemReference;

import java.util.Set;

/**
 * @author andrii.loboda
 */
public interface MsFunctionExtractionContentExpert {

    CloudStorageItemReference selectMatchingMS2Filter(Set<CloudStorageItemReference> translatedContents, final double groupPrecursor);
}
