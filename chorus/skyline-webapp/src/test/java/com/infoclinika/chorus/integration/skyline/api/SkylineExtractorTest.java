package com.infoclinika.chorus.integration.skyline.api;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.infoclinika.analysis.storage.cloud.CloudStorageFactory;
import com.infoclinika.analysis.storage.cloud.CloudStorageItemReference;
import com.infoclinika.analysis.storage.cloud.CloudStorageService;
import com.infoclinika.common.io.FileOperations;
import com.infoclinika.integration.skyline.ExtractionContentExpert;
import com.infoclinika.integration.skyline.SkylineExtractor;
import com.infoclinika.msdata.image.MzConversion;
import computations.impl.ChroExtractionClient;
import computations.impl.ComputationsMessagingService;
import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.GroupPoints;
import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.SkydWriter;
import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.chromatogramrequest.ChromSource;
import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.chromatogramrequest.ChromatogramRequestDocument;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.testng.annotations.Test;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.infoclinika.chorus.integration.skyline.api.SkylineTestUtils.removeLastSlash;

/**
 * @author Oleksii Tymchenko
 */
public class SkylineExtractorTest {

    public static final CloudStorageItemReference GAUCHER_MS1_TRANSLATED = new CloudStorageItemReference("chorus-production",
            "translated-per-file/27/2014-02/1392995866629-c15092005_002.raw.zip-0/MS-1 +p Full");
    public static final CloudStorageItemReference GAUCHER_MS2_TRANSLATED = new CloudStorageItemReference("chorus-production",
            "translated-per-file/27/2014-02/1392995866629-c15092005_002.raw.zip-0/MS2-2 +p Full");

    public static final CloudStorageItemReference UW_MSE_MS1_TRANSLATED = new CloudStorageItemReference("chorus-production",
            "translated-per-file/28/2014-11/1417182376635-2013_03_13_UWash_S1_MSE_Adj_001.raw.zip-0/MS-1 +p Full");
    public static final CloudStorageItemReference UW_MSE_MS2_TRANSLATED = new CloudStorageItemReference("chorus-production",
            "translated-per-file/28/2014-11/1417182376635-2013_03_13_UWash_S1_MSE_Adj_001.raw.zip-0/MS2-2 +p Full");


    public static final CloudStorageItemReference THERMO_DIA_MS1 = new CloudStorageItemReference("chorus-unit-tests", "skyline-translated/translated-per-file/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms [500.00-900.00]");
    public static final CloudStorageItemReference THERMO_DIA_MS2_1 = new CloudStorageItemReference("chorus-unit-tests", "skyline-translated/translated-per-file/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 510.48@hcd20.00 [72.00-1080.00]");
    public static final CloudStorageItemReference THERMO_DIA_MS2_2 = new CloudStorageItemReference("chorus-unit-tests", "skyline-translated/translated-per-file/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 530.49@hcd20.00 [74.67-1120.00]");
    public static final CloudStorageItemReference THERMO_DIA_MS2_3 = new CloudStorageItemReference("chorus-unit-tests", "skyline-translated/translated-per-file/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 550.50@hcd20.00 [77.33-1160.00]");
    public static final CloudStorageItemReference THERMO_DIA_MS2_4 = new CloudStorageItemReference("chorus-unit-tests", "skyline-translated/translated-per-file/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 570.51@hcd20.00 [80.00-1200.00]");
    public static final CloudStorageItemReference THERMO_DIA_MS2_5 = new CloudStorageItemReference("chorus-unit-tests", "skyline-translated/translated-per-file/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 590.52@hcd20.00 [83.00-1245.00]");
    public static final CloudStorageItemReference THERMO_DIA_MS2_6 = new CloudStorageItemReference("chorus-unit-tests", "skyline-translated/translated-per-file/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 610.53@hcd20.00 [85.67-1285.00]");
    public static final CloudStorageItemReference THERMO_DIA_MS2_7 = new CloudStorageItemReference("chorus-unit-tests", "skyline-translated/translated-per-file/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 630.54@hcd20.00 [88.33-1325.00]");
    public static final CloudStorageItemReference THERMO_DIA_MS2_8 = new CloudStorageItemReference("chorus-unit-tests", "skyline-translated/translated-per-file/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 650.55@hcd20.00 [91.00-1365.00]");
    public static final CloudStorageItemReference THERMO_DIA_MS2_9 = new CloudStorageItemReference("chorus-unit-tests", "skyline-translated/translated-per-file/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 670.55@hcd20.00 [93.67-1405.00]");
    public static final CloudStorageItemReference THERMO_DIA_MS2_10 = new CloudStorageItemReference("chorus-unit-tests", "skyline-translated/translated-per-file/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 690.56@hcd20.00 [96.33-1445.00]");
    public static final CloudStorageItemReference THERMO_DIA_MS2_11 = new CloudStorageItemReference("chorus-unit-tests", "skyline-translated/translated-per-file/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 710.57@hcd20.00 [99.00-1485.00]");
    public static final CloudStorageItemReference THERMO_DIA_MS2_12 = new CloudStorageItemReference("chorus-unit-tests", "skyline-translated/translated-per-file/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 730.58@hcd20.00 [102.00-1530.00]");
    public static final CloudStorageItemReference THERMO_DIA_MS2_13 = new CloudStorageItemReference("chorus-unit-tests", "skyline-translated/translated-per-file/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 750.59@hcd20.00 [104.67-1570.00]");
    public static final CloudStorageItemReference THERMO_DIA_MS2_14 = new CloudStorageItemReference("chorus-unit-tests", "skyline-translated/translated-per-file/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 770.60@hcd20.00 [107.33-1610.00]");
    public static final CloudStorageItemReference THERMO_DIA_MS2_15 = new CloudStorageItemReference("chorus-unit-tests", "skyline-translated/translated-per-file/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 790.61@hcd20.00 [110.00-1650.00]");
    public static final CloudStorageItemReference THERMO_DIA_MS2_16 = new CloudStorageItemReference("chorus-unit-tests", "skyline-translated/translated-per-file/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 810.62@hcd20.00 [112.67-1690.00]");
    public static final CloudStorageItemReference THERMO_DIA_MS2_17 = new CloudStorageItemReference("chorus-unit-tests", "skyline-translated/translated-per-file/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 830.63@hcd20.00 [115.33-1730.00]");
    public static final CloudStorageItemReference THERMO_DIA_MS2_18 = new CloudStorageItemReference("chorus-unit-tests", "skyline-translated/translated-per-file/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 850.64@hcd20.00 [118.33-1775.00]");
    public static final CloudStorageItemReference THERMO_DIA_MS2_19 = new CloudStorageItemReference("chorus-unit-tests", "skyline-translated/translated-per-file/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 870.65@hcd20.00 [121.00-1815.00]");
    public static final CloudStorageItemReference THERMO_DIA_MS2_20 = new CloudStorageItemReference("chorus-unit-tests", "skyline-translated/translated-per-file/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 890.65@hcd20.00 [123.67-1855.00]");



    public static final CloudStorageItemReference THERMO_PROD_DIA_MS1 = new CloudStorageItemReference("chorus-production", "translated-per-file/206/2014-08/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms [500.00-900.00]");
    public static final CloudStorageItemReference THERMO_PROD_DIA_MS2_1 = new CloudStorageItemReference("chorus-production", "translated-per-file/206/2014-08/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 510.48@hcd20.00 [72.00-1080.00]");
    public static final CloudStorageItemReference THERMO_PROD_DIA_MS2_2 = new CloudStorageItemReference("chorus-production", "translated-per-file/206/2014-08/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 530.49@hcd20.00 [74.67-1120.00]");
    public static final CloudStorageItemReference THERMO_PROD_DIA_MS2_3 = new CloudStorageItemReference("chorus-production", "translated-per-file/206/2014-08/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 550.50@hcd20.00 [77.33-1160.00]");
    public static final CloudStorageItemReference THERMO_PROD_DIA_MS2_4 = new CloudStorageItemReference("chorus-production", "translated-per-file/206/2014-08/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 570.51@hcd20.00 [80.00-1200.00]");
    public static final CloudStorageItemReference THERMO_PROD_DIA_MS2_5 = new CloudStorageItemReference("chorus-production", "translated-per-file/206/2014-08/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 590.52@hcd20.00 [83.00-1245.00]");
    public static final CloudStorageItemReference THERMO_PROD_DIA_MS2_6 = new CloudStorageItemReference("chorus-production", "translated-per-file/206/2014-08/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 610.53@hcd20.00 [85.67-1285.00]");
    public static final CloudStorageItemReference THERMO_PROD_DIA_MS2_7 = new CloudStorageItemReference("chorus-production", "translated-per-file/206/2014-08/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 630.54@hcd20.00 [88.33-1325.00]");
    public static final CloudStorageItemReference THERMO_PROD_DIA_MS2_8 = new CloudStorageItemReference("chorus-production", "translated-per-file/206/2014-08/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 650.55@hcd20.00 [91.00-1365.00]");
    public static final CloudStorageItemReference THERMO_PROD_DIA_MS2_9 = new CloudStorageItemReference("chorus-production", "translated-per-file/206/2014-08/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 670.55@hcd20.00 [93.67-1405.00]");
    public static final CloudStorageItemReference THERMO_PROD_DIA_MS2_10 = new CloudStorageItemReference("chorus-production", "translated-per-file/206/2014-08/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 690.56@hcd20.00 [96.33-1445.00]");
    public static final CloudStorageItemReference THERMO_PROD_DIA_MS2_11 = new CloudStorageItemReference("chorus-production", "translated-per-file/206/2014-08/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 710.57@hcd20.00 [99.00-1485.00]");
    public static final CloudStorageItemReference THERMO_PROD_DIA_MS2_12 = new CloudStorageItemReference("chorus-production", "translated-per-file/206/2014-08/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 730.58@hcd20.00 [102.00-1530.00]");
    public static final CloudStorageItemReference THERMO_PROD_DIA_MS2_13 = new CloudStorageItemReference("chorus-production", "translated-per-file/206/2014-08/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 750.59@hcd20.00 [104.67-1570.00]");
    public static final CloudStorageItemReference THERMO_PROD_DIA_MS2_14 = new CloudStorageItemReference("chorus-production", "translated-per-file/206/2014-08/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 770.60@hcd20.00 [107.33-1610.00]");
    public static final CloudStorageItemReference THERMO_PROD_DIA_MS2_15 = new CloudStorageItemReference("chorus-production", "translated-per-file/206/2014-08/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 790.61@hcd20.00 [110.00-1650.00]");
    public static final CloudStorageItemReference THERMO_PROD_DIA_MS2_16 = new CloudStorageItemReference("chorus-production", "translated-per-file/206/2014-08/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 810.62@hcd20.00 [112.67-1690.00]");
    public static final CloudStorageItemReference THERMO_PROD_DIA_MS2_17 = new CloudStorageItemReference("chorus-production", "translated-per-file/206/2014-08/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 830.63@hcd20.00 [115.33-1730.00]");
    public static final CloudStorageItemReference THERMO_PROD_DIA_MS2_18 = new CloudStorageItemReference("chorus-production", "translated-per-file/206/2014-08/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 850.64@hcd20.00 [118.33-1775.00]");
    public static final CloudStorageItemReference THERMO_PROD_DIA_MS2_19 = new CloudStorageItemReference("chorus-production", "translated-per-file/206/2014-08/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 870.65@hcd20.00 [121.00-1815.00]");
    public static final CloudStorageItemReference THERMO_PROD_DIA_MS2_20 = new CloudStorageItemReference("chorus-production", "translated-per-file/206/2014-08/20130311_DIA_Pit01.raw/FTMS + p NSI Full ms2 890.65@hcd20.00 [123.67-1855.00]");


    //    public static final String THERMO_SWATH_FILENAME = "T20131126_Study9_2_SWATH_sampleA_03.raw";
    public static final String THERMO_SWATH_FILENAME = "T20131126_Study9_2_SWATH_sampleA_01.raw";

    public static final CloudStorageItemReference THERMO_SWATH_MS1_1 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI Full ms [719.00-771.00]");
    public static final CloudStorageItemReference THERMO_SWATH_MS1_2 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI Full ms [769.00-821.00]");
    public static final CloudStorageItemReference THERMO_SWATH_MS1_3 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI Full ms [819.00-871.00]");
    public static final CloudStorageItemReference THERMO_SWATH_MS1_4 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI Full ms [869.00-921.00]");
    public static final CloudStorageItemReference THERMO_SWATH_MS1_5 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI Full ms [919.00-1000.00]");

    public static final CloudStorageItemReference THERMO_SWATH_MS2_1 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI Full ms2 413.00@hcd25.00 [200.00-1800.00]");
    public static final CloudStorageItemReference THERMO_SWATH_MS2_2 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI Full ms2 437.50@hcd25.00 [200.00-1800.00]");
    public static final CloudStorageItemReference THERMO_SWATH_MS2_3 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI Full ms2 462.50@hcd25.00 [200.00-1800.00]");
    public static final CloudStorageItemReference THERMO_SWATH_MS2_4 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI Full ms2 487.50@hcd25.00 [200.00-1800.00]");
    public static final CloudStorageItemReference THERMO_SWATH_MS2_5 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI Full ms2 512.50@hcd25.00 [200.00-1800.00]");
    public static final CloudStorageItemReference THERMO_SWATH_MS2_6 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI Full ms2 537.50@hcd25.00 [200.00-1800.00]");
    public static final CloudStorageItemReference THERMO_SWATH_MS2_7 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI Full ms2 562.50@hcd25.00 [200.00-1800.00]");
    public static final CloudStorageItemReference THERMO_SWATH_MS2_8 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI Full ms2 587.50@hcd25.00 [200.00-1800.00]");
    public static final CloudStorageItemReference THERMO_SWATH_MS2_9 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI Full ms2 615.00@hcd25.00 [200.00-1800.00]");
    public static final CloudStorageItemReference THERMO_SWATH_MS2_10 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI Full ms2 645.00@hcd25.00 [200.00-1800.00]");
    public static final CloudStorageItemReference THERMO_SWATH_MS2_11 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI Full ms2 675.00@hcd25.00 [200.00-1800.00]");
    public static final CloudStorageItemReference THERMO_SWATH_MS2_12 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI Full ms2 705.00@hcd25.00 [200.00-1800.00]");
    public static final CloudStorageItemReference THERMO_SWATH_MS2_13 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI Full ms2 745.00@hcd25.00 [200.00-1800.00]");
    public static final CloudStorageItemReference THERMO_SWATH_MS2_14 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI Full ms2 795.00@hcd25.00 [200.00-1800.00]");
    public static final CloudStorageItemReference THERMO_SWATH_MS2_15 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI Full ms2 845.00@hcd25.00 [200.00-1800.00]");
    public static final CloudStorageItemReference THERMO_SWATH_MS2_16 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI Full ms2 895.00@hcd25.00 [200.00-1800.00]");
    public static final CloudStorageItemReference THERMO_SWATH_MS2_17 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI Full ms2 959.50@hcd25.00 [200.00-1800.00]");

    public static final CloudStorageItemReference THERMO_SWATH_SIM_1 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI SIM ms [400.00-426.00]");
    public static final CloudStorageItemReference THERMO_SWATH_SIM_2 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI SIM ms [424.00-451.00]");
    public static final CloudStorageItemReference THERMO_SWATH_SIM_3 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI SIM ms [449.00-476.00]");
    public static final CloudStorageItemReference THERMO_SWATH_SIM_4 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI SIM ms [474.00-501.00]");
    public static final CloudStorageItemReference THERMO_SWATH_SIM_5 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI SIM ms [499.00-526.00]");
    public static final CloudStorageItemReference THERMO_SWATH_SIM_6 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI SIM ms [524.00-551.00]");
    public static final CloudStorageItemReference THERMO_SWATH_SIM_7 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI SIM ms [549.00-576.00]");
    public static final CloudStorageItemReference THERMO_SWATH_SIM_8 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI SIM ms [574.00-601.00]");
    public static final CloudStorageItemReference THERMO_SWATH_SIM_9 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI SIM ms [599.00-631.00]");
    public static final CloudStorageItemReference THERMO_SWATH_SIM_10 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI SIM ms [629.00-661.00]");
    public static final CloudStorageItemReference THERMO_SWATH_SIM_11 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI SIM ms [659.00-691.00]");
    public static final CloudStorageItemReference THERMO_SWATH_SIM_12 = new CloudStorageItemReference("chorus-production", "translated-per-file/216/2014-06/" + THERMO_SWATH_FILENAME + "/FTMS + p NSI SIM ms [689.00-721.00]");

    public static final CloudStorageItemReference THERMO_S_5_MS1 = new CloudStorageItemReference("chorus-production", "translated-per-file/20/2014-06/S_5.RAW/FTMS + p NSI Full ms [400.00-1400.00]");
    public static final CloudStorageItemReference THERMO_S_5_MS2 = new CloudStorageItemReference("chorus-production", "translated-per-file/20/2014-06/S_5.RAW/MS2_DDA-ITMS");

    public static final CloudStorageItemReference FAILED_THERMO_12FEB_MS1 = new CloudStorageItemReference("chorus-production", "translated-per-file/226/2015-01/C20141217_LINCS_P100_CrudevsPure_Standards_P-0013_A01_acq_01.raw/FTMS + p NSI Full ms [300.00-1200.00]");
    public static final CloudStorageItemReference FAILED_THERMO_12FEB_MS2 = new CloudStorageItemReference("chorus-production", "translated-per-file/226/2015-01/C20141217_LINCS_P100_CrudevsPure_Standards_P-0013_A01_acq_01.raw/MS2_DDA-FTMS");
    public static final Function<CloudStorageItemReference, String> DELIMITED_PATH_TRANSFORMER = new Function<CloudStorageItemReference, String>() {
        @Override
        public String apply(CloudStorageItemReference cloudStorageItemReference) {
            return removeLastSlash(cloudStorageItemReference.asDelimitedPath());
        }
    };


    private static ChromatogramExtractor getSkylineExtractor() {
        final ComputationsMessagingService messagingService = new ComputationsMessagingService();
        final ChroExtractionClient chroExtractionClient = new
                ChroExtractionClient(messagingService.messagingClientForQueue(
                "tymchenko.skyline.extraction.queue",
                "tymchenko.skyline.extraction.queue-replies",
                "tymchenko.skyline.extraction.queue-removes"));
        return new SkylineExtractor(chroExtractionClient, Files.createTempDir().getAbsolutePath(), 1, 1);
    }

    //@Test
    public void testUWashMseDataExtraction() throws Exception {
        final ChromatogramExtractor skylineExtractor = getSkylineExtractor();
        final String sampleRequest = SkylineTestUtils.readRequestFromFile("Waters_MSe.chorusrequest.xml");
        final ChromatogramRequestDocument requestDocument = SkylineTestUtils.parseRequest(sampleRequest);
        final Iterable<GroupPoints> extractedPoints = skylineExtractor.extract(
                Sets.newHashSet(UW_MSE_MS1_TRANSLATED.asDelimitedPath()),
                Sets.newHashSet(UW_MSE_MS2_TRANSLATED.asDelimitedPath()),
                Sets.<String>newHashSet(),
                requestDocument
        );
        Assert.assertNotNull(extractedPoints);
        Assert.assertTrue(extractedPoints.iterator().hasNext());
    }


    @Test(enabled = false)
    public void testThermoDiaExtraction() throws InterruptedException {
        final int jobCount = 10;
        final ExecutorService executorService = Executors.newFixedThreadPool(jobCount);
        final List<Callable<Object>> jobs = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            final int jobIdx = i;
            jobs.add(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    final long start = System.currentTimeMillis();
                    extractAndCheck("Thermo_DIA_fragment.chorusrequest.xml");
                    final long finish = System.currentTimeMillis();
                    System.out.println("Job #" + jobIdx + " completed in " + (finish - start) + "ms.");
                    return null;
                }
            });
        }
        final List<Future<Object>> ignored = executorService.invokeAll(jobs);
    }

    private void extractAndCheck(String sampleRequestFile) {
        final ChromatogramExtractor skylineExtractor = getSkylineExtractor();
//            final String sampleRequest = SkylineTestUtils.readRequestFromFile("Thermo_DIA.chorusrequest.xml");
        final String sampleRequest = SkylineTestUtils.readRequestFromFile(sampleRequestFile);
        final ChromatogramRequestDocument requestDocument = SkylineTestUtils.parseRequest(sampleRequest);
        final Iterable<GroupPoints> extractedPoints = skylineExtractor.extract(
                Sets.newHashSet(THERMO_PROD_DIA_MS1.asDelimitedPath()),
                Sets.newHashSet(
                        THERMO_PROD_DIA_MS2_1.asDelimitedPath(),
                        THERMO_PROD_DIA_MS2_2.asDelimitedPath(),
                        THERMO_PROD_DIA_MS2_3.asDelimitedPath(),
                        THERMO_PROD_DIA_MS2_4.asDelimitedPath(),
                        THERMO_PROD_DIA_MS2_5.asDelimitedPath(),
                        THERMO_PROD_DIA_MS2_6.asDelimitedPath(),
                        THERMO_PROD_DIA_MS2_7.asDelimitedPath(),
                        THERMO_PROD_DIA_MS2_8.asDelimitedPath(),
                        THERMO_PROD_DIA_MS2_9.asDelimitedPath(),
                        THERMO_PROD_DIA_MS2_10.asDelimitedPath(),
                        THERMO_PROD_DIA_MS2_11.asDelimitedPath(),
                        THERMO_PROD_DIA_MS2_12.asDelimitedPath(),
                        THERMO_PROD_DIA_MS2_13.asDelimitedPath(),
                        THERMO_PROD_DIA_MS2_14.asDelimitedPath(),
                        THERMO_PROD_DIA_MS2_15.asDelimitedPath(),
                        THERMO_PROD_DIA_MS2_16.asDelimitedPath(),
                        THERMO_PROD_DIA_MS2_17.asDelimitedPath(),
                        THERMO_PROD_DIA_MS2_18.asDelimitedPath(),
                        THERMO_PROD_DIA_MS2_19.asDelimitedPath(),
                        THERMO_PROD_DIA_MS2_20.asDelimitedPath()
                ),
                Sets.<String>newHashSet(),
                requestDocument
        );
        Assert.assertNotNull(extractedPoints);
        Assert.assertTrue(extractedPoints.iterator().hasNext());
    }

    @Test(enabled = false)
    public void testThermoSimExtraction() {
        final ChromatogramExtractor skylineExtractor = getSkylineExtractor();
        final String sampleRequest = SkylineTestUtils.readRequestFromFile("Thermo_SIM.chorusrequest.xml");
        final ChromatogramRequestDocument requestDocument = SkylineTestUtils.parseRequest(sampleRequest);
        final Iterable<GroupPoints> extractedPoints = skylineExtractor.extract(
                Sets.newHashSet(
                        THERMO_SWATH_MS1_1.asDelimitedPath(),
                        THERMO_SWATH_MS1_2.asDelimitedPath(),
                        THERMO_SWATH_MS1_3.asDelimitedPath(),
                        THERMO_SWATH_MS1_4.asDelimitedPath(),
                        THERMO_SWATH_MS1_5.asDelimitedPath()
                ),
                Sets.newHashSet(
                        THERMO_SWATH_MS2_1.asDelimitedPath(),
                        THERMO_SWATH_MS2_2.asDelimitedPath(),
                        THERMO_SWATH_MS2_3.asDelimitedPath(),
                        THERMO_SWATH_MS2_4.asDelimitedPath(),
                        THERMO_SWATH_MS2_5.asDelimitedPath(),
                        THERMO_SWATH_MS2_6.asDelimitedPath(),
                        THERMO_SWATH_MS2_7.asDelimitedPath(),
                        THERMO_SWATH_MS2_8.asDelimitedPath(),
                        THERMO_SWATH_MS2_9.asDelimitedPath(),
                        THERMO_SWATH_MS2_10.asDelimitedPath(),
                        THERMO_SWATH_MS2_11.asDelimitedPath(),
                        THERMO_SWATH_MS2_12.asDelimitedPath(),
                        THERMO_SWATH_MS2_13.asDelimitedPath(),
                        THERMO_SWATH_MS2_14.asDelimitedPath(),
                        THERMO_SWATH_MS2_15.asDelimitedPath(),
                        THERMO_SWATH_MS2_16.asDelimitedPath(),
                        THERMO_SWATH_MS2_17.asDelimitedPath()
                ),
                Sets.<String>newHashSet(
                        THERMO_SWATH_SIM_1.asDelimitedPath(),
                        THERMO_SWATH_SIM_2.asDelimitedPath(),
                        THERMO_SWATH_SIM_3.asDelimitedPath(),
                        THERMO_SWATH_SIM_4.asDelimitedPath(),
                        THERMO_SWATH_SIM_5.asDelimitedPath(),
                        THERMO_SWATH_SIM_6.asDelimitedPath(),
                        THERMO_SWATH_SIM_7.asDelimitedPath(),
                        THERMO_SWATH_SIM_8.asDelimitedPath(),
                        THERMO_SWATH_SIM_9.asDelimitedPath(),
                        THERMO_SWATH_SIM_10.asDelimitedPath(),
                        THERMO_SWATH_SIM_11.asDelimitedPath(),
                        THERMO_SWATH_SIM_12.asDelimitedPath()
                ),
                requestDocument
        );
        Assert.assertNotNull(extractedPoints);
        Assert.assertTrue(extractedPoints.iterator().hasNext());
    }

    @Test(enabled = false)
    public void testThermoS5Extraction() throws Exception {
        final ChromatogramExtractor skylineExtractor = getSkylineExtractor();
        final String sampleRequest = SkylineTestUtils.readRequestFromFile("S_5_Thermo.xml");
        final ChromatogramRequestDocument requestDocument = SkylineTestUtils.parseRequest(sampleRequest);
        final Iterable<GroupPoints> extractedPoints = skylineExtractor.extract(
                Sets.newHashSet(
                        THERMO_S_5_MS1.asDelimitedPath()
                ),
                Sets.newHashSet(
                        THERMO_S_5_MS2.asDelimitedPath()
                ),
                Sets.<String>newHashSet(),
                requestDocument
        );
        Assert.assertNotNull(extractedPoints);
        Assert.assertTrue(extractedPoints.iterator().hasNext());
        final File tempDir = Files.createTempDir();
        final File skydFile = new File(tempDir, "sample-" + System.currentTimeMillis() + ".skyd");
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(skydFile);
            SkydWriter.writeSkydFile(extractedPoints, outputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(outputStream);
            FileOperations.cleanupFile(tempDir);
        }
    }

    @Test(enabled = false)
    public void testThermoSingleSpectrumExtraction() {
        final ChromatogramExtractor skylineExtractor = getSkylineExtractor();
        final SingleSpectrumExtractionRequest singleSpectrumExtractionRequest = new SingleSpectrumExtractionRequest(
                ChromSource.MS_1, 504.7716565025, 10 * (int) MzConversion.INT
        );

        final ByteArrayOutputStream destination = new ByteArrayOutputStream();
        skylineExtractor.extractAndWrite(
                Sets.newHashSet(THERMO_DIA_MS1.asDelimitedPath()),
                Sets.newHashSet(
                        THERMO_DIA_MS2_1.asDelimitedPath(),
                        THERMO_DIA_MS2_2.asDelimitedPath(),
                        THERMO_DIA_MS2_3.asDelimitedPath(),
                        THERMO_DIA_MS2_4.asDelimitedPath(),
                        THERMO_DIA_MS2_5.asDelimitedPath(),
                        THERMO_DIA_MS2_6.asDelimitedPath(),
                        THERMO_DIA_MS2_7.asDelimitedPath(),
                        THERMO_DIA_MS2_8.asDelimitedPath(),
                        THERMO_DIA_MS2_9.asDelimitedPath(),
                        THERMO_DIA_MS2_10.asDelimitedPath(),
                        THERMO_DIA_MS2_11.asDelimitedPath(),
                        THERMO_DIA_MS2_12.asDelimitedPath(),
                        THERMO_DIA_MS2_13.asDelimitedPath(),
                        THERMO_DIA_MS2_14.asDelimitedPath(),
                        THERMO_DIA_MS2_15.asDelimitedPath(),
                        THERMO_DIA_MS2_16.asDelimitedPath(),
                        THERMO_DIA_MS2_17.asDelimitedPath(),
                        THERMO_DIA_MS2_18.asDelimitedPath(),
                        THERMO_DIA_MS2_19.asDelimitedPath(),
                        THERMO_DIA_MS2_20.asDelimitedPath()
                ),
                Sets.<String>newHashSet(),
                singleSpectrumExtractionRequest,
                destination
        );
        final String response = new String(destination.toByteArray(), Charset.defaultCharset());

        Assert.assertNotNull(response);
        System.out.println("Reponse is: " + response);
    }

    @Test(enabled = false)
    public void testFailedRequests12Feb() {
        final ChromatogramExtractor skylineExtractor = getSkylineExtractor();
        final String sampleRequest = SkylineTestUtils.readRequestFromFile("12-feb-failed-targeted-request.xml");
        final ChromatogramRequestDocument requestDocument = SkylineTestUtils.parseRequest(sampleRequest);


        final Iterable<GroupPoints> extractedPoints = skylineExtractor.extract(
                Sets.newHashSet(FAILED_THERMO_12FEB_MS1.asDelimitedPath()),
                Sets.newHashSet(FAILED_THERMO_12FEB_MS2.asDelimitedPath()),
                Sets.<String>newHashSet(),
                requestDocument
        );
        Assert.assertNotNull(extractedPoints);
        Assert.assertTrue(extractedPoints.iterator().hasNext());

    }

    @Test(enabled = false)
    public void testWatersIMSData() {

        final ChromatogramExtractor skylineExtractor = getSkylineExtractor();
        final String sampleRequest = SkylineTestUtils.readRequestFromFile("Waters-IMS-failed.chorusrequest.xml");
        final ChromatogramRequestDocument requestDocument = SkylineTestUtils.parseRequest(sampleRequest);

        hitExtractorForCloudItem(skylineExtractor, requestDocument, "chorus-production|translated-per-file/302/2015-06/1435249629096-QC_HDMSE_02_UCA168_3495_082213.raw.zip-0");
    }

    @Test(enabled = false)
    public void testAgilentAllIonsData() {
        final ChromatogramExtractor skylineExtractor = getSkylineExtractor();
//        final String sampleRequest = SkylineTestUtils.readRequestFromFile("Agilent-AllIons.chorusrequest.xml");
        final String sampleRequest = SkylineTestUtils.readRequestFromFile("20-apr-agilent-ims-oom.xml");
        final ChromatogramRequestDocument requestDocument = SkylineTestUtils.parseRequest(sampleRequest);

        hitExtractorForCloudItem(skylineExtractor, requestDocument, "chorus-production|translated-per-file/335/2016-05/1462894847363-0-BSA_Frag_100nM_18May15_Fir_15-04-02.d");
    }

    @Test(enabled = false)
    public void testTobiasData() {
        final ChromatogramExtractor skylineExtractor = getSkylineExtractor();
        final String sampleRequest = SkylineTestUtils.readRequestFromFile("9-sep-tobias.xml");
        final ChromatogramRequestDocument requestDocument = SkylineTestUtils.parseRequest(sampleRequest);

        hitExtractorForCloudItem(skylineExtractor, requestDocument, "chorus-production|translated-per-file/334/2015-06/1433194728676-20150520_12_SWATH_klk5tc_18hctrl_1.wiff.zip-0");
    }



    @Test(enabled = false)
    public void testAgilentAllIonsSingleSpectrumExtraction() {
        final ChromatogramExtractor skylineExtractor = getSkylineExtractor();
        final SingleSpectrumExtractionRequest singleSpectrumExtractionRequest = new SingleSpectrumExtractionRequest(
                ChromSource.MS_2, 662.501445506301, -1, (int) (0.04435 * MzConversion.INT)
        );

        final ByteArrayOutputStream destination = new ByteArrayOutputStream();
        hitExtractorForCloudItem(
                skylineExtractor,
                singleSpectrumExtractionRequest,
                "chorus-unit-tests|translated-per-file/all-ions/BSA-AI-0-10-25-41.d",
                destination
        );

        final String response = new String(destination.toByteArray(), Charset.defaultCharset());

        Assert.assertNotNull(response);
        System.out.println("Reponse is: " + response);
    }

    @Test(enabled = false)
    public void testFailedSingleSpectraExtraction13Oct2015() {
        final ChromatogramExtractor skylineExtractor = getSkylineExtractor();
        //https://chorusproject.org/skyline/api/chroextract-drift/file/99022/source/ms1/precursor/582.31897086/2425000
        final SingleSpectrumExtractionRequest singleSpectrumExtractionRequest = new SingleSpectrumExtractionRequest(
                ChromSource.MS_1, 582.31897086, -1, 2425000
        );

        final ByteArrayOutputStream destination = new ByteArrayOutputStream();
        hitExtractorForCloudItem(
                skylineExtractor,
                singleSpectrumExtractionRequest,
                "chorus-production|translated-per-file/335/2015-10/1444199650362-0-Yeast_0pt1ug_BSA_100nM_18May15_Fir_15-04-01.d",
                destination
        );

        final String response = new String(destination.toByteArray(), Charset.defaultCharset());

        Assert.assertNotNull(response);
        System.out.println("Reponse is: " + response);


    }

    @Test(enabled = false)
    public void testAgilentDriftTimeIMSData() {
        final ChromatogramExtractor skylineExtractor = getSkylineExtractor();
        final String sampleRequest = SkylineTestUtils.readRequestFromFile("20-sep-agilent-ims.xml");
        final ChromatogramRequestDocument requestDocument = SkylineTestUtils.parseRequest(sampleRequest);
        hitExtractorForCloudItem(skylineExtractor, requestDocument, "chorus-production|translated-per-file/335/2015-10/1443800562814-0-BSA_Frag_1nM_18May15_Fir_15-04-02.d");
    }

    @Test(enabled = false)
    public void testThermoSwathData() {
            final ChromatogramExtractor skylineExtractor = getSkylineExtractor();
            final String sampleRequest = SkylineTestUtils.readRequestFromFile("Thermo_DIA.chorusrequest.xml");
            final ChromatogramRequestDocument requestDocument = SkylineTestUtils.parseRequest(sampleRequest);
            final Iterable<GroupPoints> extractedPoints = skylineExtractor.extract(
                    Sets.newHashSet(
                            THERMO_SWATH_MS1_1.asDelimitedPath(),
                            THERMO_SWATH_MS1_2.asDelimitedPath(),
                            THERMO_SWATH_MS1_3.asDelimitedPath(),
                            THERMO_SWATH_MS1_4.asDelimitedPath(),
                            THERMO_SWATH_MS1_5.asDelimitedPath()
                    ),
                    Sets.newHashSet(
                            THERMO_SWATH_MS2_1.asDelimitedPath(),
                            THERMO_SWATH_MS2_2.asDelimitedPath(),
                            THERMO_SWATH_MS2_3.asDelimitedPath(),
                            THERMO_SWATH_MS2_4.asDelimitedPath(),
                            THERMO_SWATH_MS2_5.asDelimitedPath(),
                            THERMO_SWATH_MS2_6.asDelimitedPath(),
                            THERMO_SWATH_MS2_7.asDelimitedPath(),
                            THERMO_SWATH_MS2_8.asDelimitedPath(),
                            THERMO_SWATH_MS2_9.asDelimitedPath(),
                            THERMO_SWATH_MS2_10.asDelimitedPath(),
                            THERMO_SWATH_MS2_11.asDelimitedPath(),
                            THERMO_SWATH_MS2_12.asDelimitedPath(),
                            THERMO_SWATH_MS2_13.asDelimitedPath(),
                            THERMO_SWATH_MS2_14.asDelimitedPath(),
                            THERMO_SWATH_MS2_15.asDelimitedPath(),
                            THERMO_SWATH_MS2_16.asDelimitedPath(),
                            THERMO_SWATH_MS2_17.asDelimitedPath()
                    ),
                    Sets.<String>newHashSet(
                            THERMO_SWATH_SIM_1.asDelimitedPath(),
                            THERMO_SWATH_SIM_2.asDelimitedPath(),
                            THERMO_SWATH_SIM_3.asDelimitedPath(),
                            THERMO_SWATH_SIM_4.asDelimitedPath(),
                            THERMO_SWATH_SIM_5.asDelimitedPath(),
                            THERMO_SWATH_SIM_6.asDelimitedPath(),
                            THERMO_SWATH_SIM_7.asDelimitedPath(),
                            THERMO_SWATH_SIM_8.asDelimitedPath(),
                            THERMO_SWATH_SIM_9.asDelimitedPath(),
                            THERMO_SWATH_SIM_10.asDelimitedPath(),
                            THERMO_SWATH_SIM_11.asDelimitedPath(),
                            THERMO_SWATH_SIM_12.asDelimitedPath()
                    ),
                    requestDocument
            );
            Assert.assertNotNull(extractedPoints);
            Assert.assertTrue(extractedPoints.iterator().hasNext());
    }

    @Test(enabled = true)
    public void testThermoLoadTestingFileFromNick() {
        final Set<String> ms2Fns = Sets.newHashSet("chorus-production|translated-per-file/226/2015-05/Q_2014_0523_115_0_amol_uL_20mz.raw/FTMS + p NSI Full ms2 550.50@hcd27.00 [77.33-1160.00]",
                "chorus-production|translated-per-file/226/2015-05/Q_2014_0523_115_0_amol_uL_20mz.raw/FTMS + p NSI Full ms2 690.56@hcd27.00 [96.33-1445.00]",
                "chorus-production|translated-per-file/226/2015-05/Q_2014_0523_115_0_amol_uL_20mz.raw/FTMS + p NSI Full ms2 470.46@hcd27.00 [66.67-1000.00]",
                "chorus-production|translated-per-file/226/2015-05/Q_2014_0523_115_0_amol_uL_20mz.raw/FTMS + p NSI Full ms2 750.59@hcd27.00 [104.67-1570.00]",
                "chorus-production|translated-per-file/226/2015-05/Q_2014_0523_115_0_amol_uL_20mz.raw/FTMS + p NSI Full ms2 790.61@hcd27.00 [110.00-1650.00]",
                "chorus-production|translated-per-file/226/2015-05/Q_2014_0523_115_0_amol_uL_20mz.raw/FTMS + p NSI Full ms2 530.49@hcd27.00 [74.67-1120.00]",
                "chorus-production|translated-per-file/226/2015-05/Q_2014_0523_115_0_amol_uL_20mz.raw/FTMS + p NSI Full ms2 610.53@hcd27.00 [85.67-1285.00]",
                "chorus-production|translated-per-file/226/2015-05/Q_2014_0523_115_0_amol_uL_20mz.raw/FTMS + p NSI Full ms2 670.55@hcd27.00 [93.67-1405.00]",
                "chorus-production|translated-per-file/226/2015-05/Q_2014_0523_115_0_amol_uL_20mz.raw/FTMS + p NSI Full ms2 430.45@hcd27.00 [61.00-915.00]",
                "chorus-production|translated-per-file/226/2015-05/Q_2014_0523_115_0_amol_uL_20mz.raw/FTMS + p NSI Full ms2 410.44@hcd27.00 [58.33-875.00]",
                "chorus-production|translated-per-file/226/2015-05/Q_2014_0523_115_0_amol_uL_20mz.raw/FTMS + p NSI Full ms2 450.45@hcd27.00 [63.67-955.00]",
                "chorus-production|translated-per-file/226/2015-05/Q_2014_0523_115_0_amol_uL_20mz.raw/FTMS + p NSI Full ms2 650.55@hcd27.00 [91.00-1365.00]",
                "chorus-production|translated-per-file/226/2015-05/Q_2014_0523_115_0_amol_uL_20mz.raw/FTMS + p NSI Full ms2 770.60@hcd27.00 [107.33-1610.00]",
                "chorus-production|translated-per-file/226/2015-05/Q_2014_0523_115_0_amol_uL_20mz.raw/FTMS + p NSI Full ms2 490.47@hcd27.00 [69.33-1040.00]",
                "chorus-production|translated-per-file/226/2015-05/Q_2014_0523_115_0_amol_uL_20mz.raw/FTMS + p NSI Full ms2 510.48@hcd27.00 [72.00-1080.00]",
                "chorus-production|translated-per-file/226/2015-05/Q_2014_0523_115_0_amol_uL_20mz.raw/FTMS + p NSI Full ms2 590.52@hcd27.00 [83.00-1245.00]",
                "chorus-production|translated-per-file/226/2015-05/Q_2014_0523_115_0_amol_uL_20mz.raw/FTMS + p NSI Full ms2 730.58@hcd27.00 [102.00-1530.00]",
                "chorus-production|translated-per-file/226/2015-05/Q_2014_0523_115_0_amol_uL_20mz.raw/FTMS + p NSI Full ms2 630.54@hcd27.00 [88.33-1325.00]",
                "chorus-production|translated-per-file/226/2015-05/Q_2014_0523_115_0_amol_uL_20mz.raw/FTMS + p NSI Full ms2 710.57@hcd27.00 [99.00-1485.00]",
                "chorus-production|translated-per-file/226/2015-05/Q_2014_0523_115_0_amol_uL_20mz.raw/FTMS + p NSI Full ms2 570.51@hcd27.00 [80.00-1200.00]"
        );
        final Set<String> ms1Fns = Sets.newHashSet("chorus-production|translated-per-file/226/2015-05/Q_2014_0523_115_0_amol_uL_20mz.raw/FTMS + p NSI Full lock ms [390.00-810.00]",
                "chorus-production|translated-per-file/226/2015-05/Q_2014_0523_115_0_amol_uL_20mz.raw/FTMS + p NSI Full ms [390.00-810.00]"
        );

        final ChromatogramExtractor skylineExtractor = getSkylineExtractor();
//        final String sampleRequest = SkylineTestUtils.readRequestFromFile("20mz_DIA_1k-MS1only.ChorusRequest.xml");
        final String sampleRequest = SkylineTestUtils.readRequestFromFile("20mz_DIA_1k.ChorusRequest.xml");
        final ChromatogramRequestDocument requestDocument = SkylineTestUtils.parseRequest(sampleRequest);
        final Iterable<GroupPoints> extractedPoints = skylineExtractor.extract(ms1Fns, ms2Fns, Sets.newHashSet(), requestDocument);
        Assert.assertNotNull(extractedPoints);
        Assert.assertTrue(extractedPoints.iterator().hasNext());

    }


    private static void hitExtractorForCloudItem(ChromatogramExtractor skylineExtractor,
                                                 SingleSpectrumExtractionRequest request,
                                                 String cloudItem,
                                                 OutputStream destination) {
        final CloudStorageItemReference rootFolder = CloudStorageItemReference.parseFileReference(cloudItem);
        final CloudStorageService storageService = CloudStorageFactory.service();

        final List<CloudStorageItemReference> allChildItems = storageService.list(rootFolder.getBucket(), rootFolder.getKey(), Optional.<Date>absent());
        final TreeSet<CloudStorageItemReference> allMs1Fns = new TreeSet<>();
        final Set<CloudStorageItemReference> allMs2Fns = new TreeSet<>();

        for (CloudStorageItemReference childItem : allChildItems) {
            final CloudStorageItemReference parentRef = childItem.parentReference();
            if (parentRef.getKey().contains(ExtractionContentExpert.GLOBAL_FN_PREFIX_MS1)) {
                allMs1Fns.add(parentRef);
            } else if (parentRef.getKey().contains(ExtractionContentExpert.GLOBAL_FN_PREFIX_MS2)) {
                allMs2Fns.add(parentRef);
            }
        }

        System.out.println("MS1 fns (total " + allMs1Fns.size() + "): " + Joiner.on(",").join(allMs1Fns));
        System.out.println("MS2 fns (total " + allMs2Fns.size() + "): " + Joiner.on(",").join(allMs2Fns));


        skylineExtractor.extractAndWrite(
                Sets.newHashSet(Iterables.transform(allMs1Fns, DELIMITED_PATH_TRANSFORMER)),
                Sets.newHashSet(Iterables.transform(allMs2Fns, DELIMITED_PATH_TRANSFORMER)),
                Sets.<String>newHashSet(),
                request,
                destination);
    }


    private static void hitExtractorForCloudItem(ChromatogramExtractor skylineExtractor, ChromatogramRequestDocument requestDocument, String cloudItem) {
        final CloudStorageItemReference rootFolder = CloudStorageItemReference.parseFileReference(cloudItem);
        final CloudStorageService storageService = CloudStorageFactory.service();

        System.out.println("Listing items from the " + rootFolder.asDelimitedPath());

        final List<CloudStorageItemReference> allChildItems = storageService.list(rootFolder.getBucket(), rootFolder.getKey(), Optional.<Date>absent());
        final TreeSet<CloudStorageItemReference> allMs1Fns = new TreeSet<>();
        final Set<CloudStorageItemReference> allMs2Fns = new TreeSet<>();

        for (CloudStorageItemReference childItem : allChildItems) {
            final CloudStorageItemReference parentRef = childItem.parentReference();
            if (parentRef.getKey().contains(ExtractionContentExpert.GLOBAL_FN_PREFIX_MS1)) {
                allMs1Fns.add(parentRef);
            } else if (parentRef.getKey().contains(ExtractionContentExpert.GLOBAL_FN_PREFIX_MS2)) {
                allMs2Fns.add(parentRef);
            }
        }

        System.out.println("MS1 fns (total " + allMs1Fns.size() + "): " + Joiner.on(",").join(allMs1Fns));
        System.out.println("MS2 fns (total " + allMs2Fns.size() + "): " + Joiner.on(",").join(allMs2Fns));


        final Iterable<GroupPoints> extractedPoints = skylineExtractor.extract(
                Sets.newHashSet(Iterables.transform(allMs1Fns, DELIMITED_PATH_TRANSFORMER)),
                Sets.newHashSet(Iterables.transform(allMs2Fns, DELIMITED_PATH_TRANSFORMER)),
                Sets.<String>newHashSet(),
                requestDocument
        );
        Assert.assertNotNull(extractedPoints);
        Assert.assertTrue(extractedPoints.iterator().hasNext());
    }

}
