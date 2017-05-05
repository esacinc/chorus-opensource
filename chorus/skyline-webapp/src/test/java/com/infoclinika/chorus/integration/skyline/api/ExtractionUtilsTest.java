package com.infoclinika.chorus.integration.skyline.api;

import com.infoclinika.analysis.storage.cloud.CloudStorageItemReference;
import com.infoclinika.common.utils.Pair;
import com.infoclinika.integration.skyline.ExtractionUtils;
import com.infoclinika.msdata.image.MzConversion;
import junit.framework.Assert;
import org.testng.annotations.Test;

/**
 * @author Oleksii Tymchenko
 */
public class ExtractionUtilsTest {

    @Test
    public void testRangeParsing() {
        final Pair<Double,Double> extractedRange = ExtractionUtils.parseRange(new CloudStorageItemReference("bucket", "translated-per-file/216/2014-06/T20131126_Study9_2_SWATH_sampleA_03.raw/" +
                "FTMS + p NSI Full ms [719.00-771.00]"));
        Assert.assertNotNull(extractedRange);
        Assert.assertEquals(719.00d, extractedRange.getFirst());
        Assert.assertEquals(771.00d, extractedRange.getSecond());
    }

    @Test
    public void testPackingOfRtAndFnIndex() {
        int rt = (int) (240 * MzConversion.INT);
        int index = 247;
        final long packedValue = ExtractionUtils.packToInt(index, rt);
        final Pair<Integer, Integer> indexRtPair = ExtractionUtils.unpackValue(packedValue);
        final Integer unpackedIndex = indexRtPair.getFirst();
        final Integer unpackedRt = indexRtPair.getSecond();

        Assert.assertEquals(rt, unpackedRt.intValue());
        Assert.assertEquals(index, unpackedIndex.intValue());
    }
}
