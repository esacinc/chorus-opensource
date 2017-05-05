package com.infoclinika.mssharing.extraction;

import com.infoclinika.analysis.storage.cloud.CloudStorageItemReference;
import com.infoclinika.mssharing.model.extraction.MsFunctionExtractionContentExpert;
import com.infoclinika.mssharing.model.extraction.exception.NoMatchingFilterForPrecursorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author andrii.loboda, Moved from com.infoclinika.integration.skyline.AbstractExtractionJob
 */
public class MsFunctionExtractionContentExpertImpl implements MsFunctionExtractionContentExpert {
    private static final Logger LOG = LoggerFactory.getLogger(MsFunctionExtractionContentExpertImpl.class);

    @Override
    public CloudStorageItemReference selectMatchingMS2Filter(Set<CloudStorageItemReference> translatedContents, double groupPrecursor) {
        if (translatedContents.size() == 1) {
            return translatedContents.iterator().next();
        }
        final Map<Double, CloudStorageItemReference> precursorToFilter = new HashMap<>();
        for (CloudStorageItemReference filterRef : translatedContents) {
            final Double precursor = parsePrecursor(filterRef);
            if (precursor != null) {
                precursorToFilter.put(precursor, filterRef);
            }
        }

        final List<Double> precursors = new ArrayList<>(precursorToFilter.keySet());

        Collections.sort(precursors, new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                final double val1 = Math.abs(o1 - groupPrecursor);
                final double val2 = Math.abs(o2 - groupPrecursor);
                return Double.compare(val1, val2);
            }
        });
        final Double closestPrecursor = precursors.get(0);
        final CloudStorageItemReference filter = precursorToFilter.get(closestPrecursor);

        if (filter == null) {
            throw new NoMatchingFilterForPrecursorException(groupPrecursor);
        }
        LOG.debug("--- Found closest filter. Precursor: " + groupPrecursor + ". Filter: " + filter.asDelimitedPath());
        return filter;
    }

    private static Double parsePrecursor(CloudStorageItemReference filterRef) {
        try {
            final String key = filterRef.getKey();
            final int atIndex = key.indexOf("@");
            final String beforeAt = key.substring(0, atIndex);
            final int lastSpace = beforeAt.lastIndexOf(" ");
            final String precursorStr = beforeAt.substring(lastSpace + 1);
            return Double.parseDouble(precursorStr);
        } catch (NumberFormatException e) {
            LOG.warn("Cannot parse precursor value from: " + filterRef.asDelimitedPath());
            return null;
        }
    }
}
