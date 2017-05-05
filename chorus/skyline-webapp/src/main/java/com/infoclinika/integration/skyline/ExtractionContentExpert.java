package com.infoclinika.integration.skyline;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.infoclinika.analysis.storage.cloud.CloudStorageItemReference;
import com.infoclinika.common.utils.Pair;
import org.apache.log4j.Logger;

import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newTreeSet;

/**
 * @author Oleksii Tymchenko
 */
public class ExtractionContentExpert {
    private static final Logger LOGGER = Logger.getLogger(ExtractionContentExpert.class);

    public static final String DRIFT_PREFIX = "drift_";
    public static final String COLLISION_ENERGY_PREFIX = "ce_";
    public static final String GLOBAL_FN_PREFIX_MS1 = "MS-";
    public static final String GLOBAL_FN_PREFIX_MS2 = "MS2-";
    public static final double DRIFT_TIME_SIMILARITY_DIFFERENCE = 0.001;

    public static int getDriftFnCount(Set<CloudStorageItemReference> ms1TranslatedContent, Set<CloudStorageItemReference> ms2TranslatedContent) {
        int totalDriftFns = 0;
        for (CloudStorageItemReference ms1Filter : ms1TranslatedContent) {
            if (ms1Filter.asDelimitedPath().contains(DRIFT_PREFIX)) {
                totalDriftFns++;
            }
        }

        if (totalDriftFns == 0) {
            for (CloudStorageItemReference ms2Filter : ms2TranslatedContent) {
                if (ms2Filter.asDelimitedPath().contains(DRIFT_PREFIX)) {
                    totalDriftFns++;
                }
            }
        }
        return totalDriftFns;
    }

    static CloudStorageItemReference findPrecalculatedContent(Set<CloudStorageItemReference> translatedContent, String prefix) {
        CloudStorageItemReference globalFn = null;
        for (CloudStorageItemReference Ref : translatedContent) {
            if (!Ref.getKey().contains(DRIFT_PREFIX) && Ref.getKey().contains(prefix)) {
                globalFn = Ref;
            }
        }
        return globalFn;
    }

    public static SortedMap<Double, Pair<CloudStorageItemReference, CloudStorageItemReference>> layoutByDriftTime(
            Set<CloudStorageItemReference> ms1Contents,
            Set<CloudStorageItemReference> ms2Contents
    ) {
        SortedMap<Double, Pair<CloudStorageItemReference, CloudStorageItemReference>> results = new TreeMap<>();
        Map<Double, CloudStorageItemReference> ms1LaidOut = layoutByDriftTime(ms1Contents);
        Map<Double, CloudStorageItemReference> ms2LaidOut = layoutByDriftTime(ms2Contents);
        for (Double ms1DriftTime : ms1LaidOut.keySet()) {
            final CloudStorageItemReference matchingMs2 = ms2LaidOut.get(ms1DriftTime);
            if (matchingMs2 == null) {
                LOGGER.warn("Cannot find matching MS2 for drift time " + ms1DriftTime);
            } else {
                results.put(ms1DriftTime, new Pair<>(ms1LaidOut.get(ms1DriftTime), matchingMs2));
            }
        }
        return results;
    }

    public static Map<Double, CloudStorageItemReference> layoutByDriftTime(Set<CloudStorageItemReference> contents) {
        final Map<Double, CloudStorageItemReference> result = new HashMap<>();
        for (CloudStorageItemReference item : contents) {
            final Double driftTime = parseDriftTime(item);
            if (driftTime != null) {
                result.put(driftTime, item);
            }
        }
        return result;
    }

    public static Pair<CloudStorageItemReference, CloudStorageItemReference> findExactPair(
            SortedMap<Double, Pair<CloudStorageItemReference, CloudStorageItemReference>> driftTimeToFns,
            Double driftTime) {
        Pair<CloudStorageItemReference, CloudStorageItemReference> result = null;

        for (Double candidateDriftTime : driftTimeToFns.keySet()) {
            //todo[tymchenko]: calculate the drift similarity
            if (Math.abs(candidateDriftTime - driftTime) < DRIFT_TIME_SIMILARITY_DIFFERENCE) {
                result = driftTimeToFns.get(candidateDriftTime);
            }
        }
        return result;
    }

    public static Double parseDriftTime(CloudStorageItemReference translatedFilterRef) {
        return parseValueFromFunctionPath(translatedFilterRef, DRIFT_PREFIX, "Error parsing drift time from filter: " + translatedFilterRef);
    }

    public static Double parseCEValue(CloudStorageItemReference ms2FilterRef) {
        return parseValueFromFunctionPath(ms2FilterRef, COLLISION_ENERGY_PREFIX, "Error parsing collision energy from filter: " + ms2FilterRef);
    }

    private static Double parseValueFromFunctionPath(CloudStorageItemReference translatedFilterRef, String anchorPrefix, String errorMessage) {
        Double parsedValue = null;
        try {
            final String key = translatedFilterRef.getKey();
            final int startPrefix = key.indexOf(anchorPrefix);
            if (startPrefix >= 0) {
                String partialString = key.substring(startPrefix + anchorPrefix.length());
                if (partialString.contains(CloudStorageItemReference.CLOUD_REFERENCE_URL_SEPARATOR)) {
                    partialString = partialString.substring(0, partialString.indexOf(CloudStorageItemReference.CLOUD_REFERENCE_URL_SEPARATOR));
                }
                parsedValue = Double.parseDouble(partialString);
            }
        } catch (NumberFormatException e) {
            LOGGER.warn(errorMessage, e);
        }
        return parsedValue;
    }

    protected static boolean detectCollisionEnergyData(Set<CloudStorageItemReference> ms2TranslatedContent) {
        boolean isCEData = false;
        for (CloudStorageItemReference ms2Item : ms2TranslatedContent) {
            if (ms2Item.asDelimitedPath().contains(COLLISION_ENERGY_PREFIX)) {
                isCEData = true;
            }
        }
        return isCEData;
    }

    public static Set<CloudStorageItemReference> pickFirstFn(Set<CloudStorageItemReference> contents) {
        final CloudStorageItemReference alphabeticalFirst = (newTreeSet(contents)).iterator().next();
        return Sets.newHashSet(alphabeticalFirst);
    }

    public static Set<CloudStorageItemReference> parseMultipleRefs(Set<String> filterRefs) {
        if(filterRefs == null) {
            return new HashSet<>();
        }
        boolean containsValidStrings = false;
        for (String filterRef : filterRefs) {
            if(filterRef != null && filterRef.trim().length() > 0) {
                containsValidStrings = true;
            }
        }

        if(!containsValidStrings) {
            return new HashSet<>();
        }

        return newHashSet(Iterables.transform(filterRefs, new Function<String, CloudStorageItemReference>() {
            @Override
            public CloudStorageItemReference apply(String ref) {
                return CloudStorageItemReference.parseFileReference(ref);
            }
        }));
    }

    public static Set<String> removeUnwantedFilters(Set<String> rawFilters) {
        return new TreeSet<>(Sets.filter(rawFilters, s -> !s.toLowerCase().contains(" lock ")));
    }
}
