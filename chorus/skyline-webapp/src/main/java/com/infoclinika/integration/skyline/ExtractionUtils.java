package com.infoclinika.integration.skyline;

import com.infoclinika.analysis.storage.cloud.CloudStorageItemReference;
import com.infoclinika.common.utils.Pair;
import org.apache.log4j.Logger;

/**
 * @author Oleksii Tymchenko
 */
public class ExtractionUtils {
    private static final Logger LOGGER = Logger.getLogger(ExtractionUtils.class);

    public static Pair<Double, Double> parseRange(CloudStorageItemReference filter) {
        final String key = filter.getKey();
        try {
            final int startBraceIndex = key.indexOf("[");
            final int endBraceIndex = key.indexOf("]");
            final String rangesString = key.substring(startBraceIndex + 1, endBraceIndex);
            final String[] rawRanges = rangesString.split("-");
            final double rangeStart = Double.parseDouble(rawRanges[0]);
            final double rangeEnd = Double.parseDouble(rawRanges[1]);
            return new Pair<>(rangeStart, rangeEnd);
        } catch (Exception e) {
            LOGGER.error("Cannot parse MZ ranges from string: " + key, e);
            return null;
        }
    }

    public static long packToInt(int sortedIndex, int rt) {
        long sortedIndexLong = sortedIndex;
        final long shiftedIndex = sortedIndexLong << 32;
        return shiftedIndex | rt;
    }

    public static Pair<Integer, Integer> unpackValue(long packedValue) {
        final long index = packedValue >> 32;
        final long rt = packedValue & 0x0ffffffff;
        return new Pair<>((int)index, (int) rt);
    }
}
