package com.infoclinika.chorus.integration.skyline.api;

import com.infoclinika.msdata.image.MzConversion;
import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.chromatogramrequest.ChromatogramRequestDocument;
import junit.framework.Assert;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.testng.annotations.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Oleksii Tymchenko
 */
public class SkylineProductionIntegrationTest {

    private static final Logger LOGGER = Logger.getLogger(SkylineProductionIntegrationTest.class);

    private static final String AUTH_HEADER = "Authorization";
    private static final String BASE_URL = "https://chorusproject.org/skyline/api/";
    private static final String STAGING_BASE_URL = "https://dev.chorusproject.org/skyline/api/";
//    private static final String BASE_URL = "http://localhost:8080/skyline/api/";

    @Test(enabled = false)
    public void testSingleSpectrumExtractionOnProduction() throws IOException {

        final int fileId = 28836; //20130311_DIA_Pit01.raw;


        final String source = "Ms1";
        final String precursor = "504.7716565025";
        final int sourceRt = 10;

        extractSingleSpectrum(fileId, source, precursor, sourceRt);
    }

    @Test(enabled = false)
    public void testSingleSpectrumExtractionOnDevStaging() throws IOException {

        final int fileId = 7; //20130311_DIA_Pit01.raw;


        final String source = "Ms1";
        final String precursor = "504.7716565025";
        final int sourceRt = 10;

        extractSingleSpectrum(fileId, source, precursor, sourceRt, STAGING_BASE_URL);
    }


    @Test(enabled = false)
    public void testHoofnagleDiaSet() throws InterruptedException {
        final String sampleRequest = SkylineTestUtils.readRequestFromFile("Thermo_DIA.chorusrequest.xml");

        hitExtractor(sampleRequest, new int[]{28836, 28837, 28838, 28839, 28840, 28841}, 1, 1);
    }

    @Test(enabled = false)
    public void testThermoSwathSet() throws InterruptedException {
        final String sampleRequest = SkylineTestUtils.readRequestFromFile("Thermo_SWATH.chorusrequest.xml");

//        hitExtractor(sampleRequest,
//                new int[]{36294, 36295, 36296, 36297, 36298, 36299, 36300, 36301, 36302, 36303,
//                        36304, 36305, 36306, 36307, 36308, 36309, 36310, 36311, 36312, 36313,
//                        36314, 36315, 36316, 36317, 36318, 36319, 36320, 36321, 36322},
//                10, 1);
        hitExtractor(sampleRequest,
                new int[]{36321},
                1, 1);
    }

    @Test(enabled = false)
    public void testMissingChros() throws InterruptedException {
        final String sampleRequest = SkylineTestUtils.readRequestFromFile("30-mar-failed-request.xml");
        hitExtractor(sampleRequest, new int[]{62306}, 1, 1);

    }

    @Test(enabled = false)
    public void testWrongHugeIntensity() throws InterruptedException {
        final String sampleRequest = SkylineTestUtils.readRequestFromFile("2-apr-huge-intensity.xml");
        hitExtractor(sampleRequest, new int[]{53372}, 1, 1);

    }

    @Test(enabled = false)
    public void testHugeRequests16Apr() throws InterruptedException {
        final String sampleRequest = SkylineTestUtils.readRequestFromFile("16-apr-dead-service.xml");
        hitExtractor(sampleRequest,
                new int[]{36294, 36295, 36296, 36297, 36298, 36299, 36300, 36301, 36302, 36303,
                        36304, 36305, 36306, 36307, 36308, 36309, 36310, 36311, 36312, 36313,
                        36314, 36315, 36316, 36317, 36318, 36319, 36320, 36321, 36322},
                10, 1);
    }

    @Test(enabled = false)
    public void testBirgitSciexData() throws InterruptedException {
        final String sampleRequest = SkylineTestUtils.readRequestFromFile("21-apr-sciex.xml");
        hitExtractor(sampleRequest, new int[]{40504}, 1, 1);

    }


    @Test(enabled = false)
    public void testSciexBirgitData6May() throws InterruptedException {
        final String sampleRequest = SkylineTestUtils.readRequestFromFile("6-may-sciex-birgit-data.xml");
        hitExtractor(sampleRequest,
                new int[]{39740
//                        , 39739, 76737, 40494, 76731, 76732, 40514, 40504, 40505, 40507, 40509, 40510
                },
                1, 1);

    }

    @Test(enabled = false)
    public void testWatersMSeExtraction() throws InterruptedException {
        final String sampleRequest = SkylineTestUtils.readRequestFromFile("Waters_MSe.chorusrequest.xml");
        hitExtractor(sampleRequest,
                new int[]{43342},
                1, 1);

    }

    @Test(enabled = false)
    public void testWatersIMSExtraction() throws InterruptedException {
        final String sampleRequest = SkylineTestUtils.readRequestFromFile("Waters-IMS-failed.chorusrequest.xml");
        hitExtractor(sampleRequest,
                new int[]{82909},
                1, 1);

    }

    @Test(enabled = false)
    public void testWatersImsSingleSpectrumExtractionOnProduction() throws IOException {
        final int fileId = 82909; //QC_HDMSE_02_UCA168_3495_082213.raw;
        final String source = "Ms1";
        final String precursor = "484.7454417325";
        final int sourceRt = 10;

        extractSingleSpectrumWithDriftTime(fileId, source, precursor, sourceRt, 10.280825225971d);
    }

    @Test(enabled = false)
    public void testAgilentAllIonsExtraction() throws InterruptedException {
        //BSA-AI-0-10-25-41.d;
        final String sampleRequest = SkylineTestUtils.readRequestFromFile("Agilent-AllIons.chorusrequest.xml");
        hitExtractor(sampleRequest,
                new int[]{83479},
                1, 1);

    }

    @Test(enabled = false)
    public void testAgilentAllIonsSingleSpectrumExtractionOnProduction() throws IOException {
        final int fileId = 83479; //BSA-AI-0-10-25-41.d;
        final String source = "Ms2";
        final String precursor = "662.501445506301";
        final int sourceRt = 4435;

        //drift time = .010280825225971
        extractSingleSpectrum(fileId, source, precursor, sourceRt);
    }

    @Test(enabled = false)
    public void testAgilentImsExperiment1962() throws Exception {

        final String sampleRequest = SkylineTestUtils.readRequestFromFile("20-sep-agilent-ims.xml");
        hitExtractor(sampleRequest,
                new int[]{
                        98990 //BSA_Frag_10pM_18May15_Fir_15-04-02.d.zip
                        ,98992 //BSA_Frag_1pM_a_18May15_Fir_15-04-02.d.zip
                        ,98989 //BSA_Frag_10nM_18May15_Fir_15-04-02.d.zip
                        ,98998 //BSA_Frag_1nM_18May15_Fir_15-04-02.d.zip
                },
                1, 1);

    }

    @Test(enabled = false)
    public void testAgilentImsOutOfMemoryExtraction() throws InterruptedException {
        final String sampleRequest = SkylineTestUtils.readRequestFromFile("20-apr-agilent-ims-oom.xml");
        hitExtractor(sampleRequest,
                new int[]{
                        98997 //raw-files/86/335/BSA_Frag_100nM_18May15_Fir_15-04-02.d.zip
                },
                1, 1);
    }


    @Test(enabled = false)
    public void testPrmFails() throws Exception {

        final String sampleRequest = SkylineTestUtils.readRequestFromFile("16-mar-prm-fails.xml");
        hitExtractor(sampleRequest,
                new int[]{
                        36321
                },
                1, 1);

    }

    @Test(enabled = false)
    public void testPrmPerformance() throws Exception {

        final String sampleRequest = SkylineTestUtils.readRequestFromFile("16-mar-prm-fails.xml");
        //experiment 2250 (Plate35Test). Files are 122355-122380 of the form C20160301_P100_Plate35_*
        hitExtractor(sampleRequest,
                new int[]{
                        122355,
                        122356,
                        122357,
                        122358,
                        122359,
                        122360,
                        122361,
                        122362,
                        122363,
                        122364,
                        122365,
                        122366,
                        122367,
                        122368,
                        122369,
                        122370,
                        122371,
                        122372,
                        122373,
                        122374,
                        122375,
                        122376,
                        122377,
                        122378,
                        122379,
                        122380
                },
                1, 1);

    }

    @Test(enabled = false)
    public void testSkylineLoad1kTransitions() throws Exception {

        final String sampleRequest = SkylineTestUtils.readRequestFromFile("20mz_DIA_1k.ChorusRequest.xml");
        hitExtractor(sampleRequest,
                new int[]{
                        39879 //Q_2014_0523_115_0_amol_uL_20mz.raw
                },
                1, 1);

    }

    private List<String> splitFileIntoBlocks(String requestFileName, int blockSize) throws Exception{
        final List<String> blocks = new ArrayList<>(120);
        final String sampleRequest = SkylineTestUtils.readRequestFromFile(requestFileName);
        final ChromatogramRequestDocument requestDocument = SkylineTestUtils.parseRequest(sampleRequest);
        final List<ChromatogramRequestDocument.ChromatogramGroup> requestChromatogramGroups = requestDocument.getChromatogramGroup();
        final List<ChromatogramRequestDocument.ChromatogramGroup> allChromatogramGroups = new ArrayList<>(requestChromatogramGroups);
        final Iterator<ChromatogramRequestDocument.ChromatogramGroup> groupIterator = allChromatogramGroups.iterator();

        int blockCounter = 1;
        while (groupIterator.hasNext()) {
            requestChromatogramGroups.clear();
            int chromatCount = 0;
            while (chromatCount < blockSize && groupIterator.hasNext()) {
                ChromatogramRequestDocument.ChromatogramGroup group = groupIterator.next();
                requestChromatogramGroups.add(group);
                chromatCount += group.getChromatogram().size();
            }
            String block = SkylineTestUtils.generateRequest(requestDocument);
            blocks.add(block);

            // Save partial requests to temporary files for debug
            PrintWriter printWriter = new PrintWriter("/tmp/out/res/cromats" + String.format("%03d", blockCounter) + ".xml");
            printWriter.append(block);
            printWriter.close();
            blockCounter++;
        }
        return blocks;
    }

    public List<JobResult> runSkylineLoadForPartialTransitions(final int parallelism, final int jobsForOneRequest, final List<String> subRequests, final int[] sourceFileIds) throws Exception {
        final List<Callable<List<JobResult>>> jobs = new ArrayList<>(subRequests.size());
        int blockCounter = 1;
        for (String subRequest : subRequests) {
            final int blockNumber = blockCounter++;
            jobs.add(() -> {
                        return hitExtractor(subRequest, sourceFileIds, jobsForOneRequest, 1, blockNumber, BASE_URL);
                    }
            );
        }

        final ExecutorService executorService = Executors.newFixedThreadPool(parallelism);
        final List<Future<List<JobResult>>> results = executorService.invokeAll(jobs);
        executorService.shutdownNow();

        final List<JobResult> commonJobResultList = new ArrayList<>();
        for (Future<List<JobResult>> jobResultFuture: results){
            final List<JobResult> jobResultList = jobResultFuture.get();
            commonJobResultList.addAll(jobResultList);
        }

        return commonJobResultList;
    }

    private void checkAndLogJobResults (List<JobResult> jobResultList){
        int failedJobsCount = 0;
        StringWriter failedJobWriter = new StringWriter();
        long minResponseTime = Long.MAX_VALUE;
        long maxResponseTime = 0;
        long sum = 0;

        for (JobResult jobResult : jobResultList) {
            long requestTime = jobResult.getTime();
            if (minResponseTime > requestTime) minResponseTime = requestTime;
            if (maxResponseTime < requestTime) maxResponseTime = requestTime;
            sum += requestTime;
            if (jobResult.isFailed()) {
                failedJobWriter.append("\n\n").append(jobResult.getFailedJobInfo());
                failedJobsCount++;
            }
        }

        int jobQuantity = jobResultList.size();
        LOGGER.info("\n> Mean time: " + ((double) sum / jobQuantity) +
                        "\n  > Max time: " + maxResponseTime + "" +
                        "\n  > Min time: " + minResponseTime
        );

        if (failedJobsCount > 0) {
            String failureInfo = failedJobWriter.toString();
            Assert.fail("There are " + failedJobsCount + " failures of " + jobQuantity + " requests. Failure details: " + failureInfo);
        }
    }

@Test(enabled = false)
    public void testSkylineLoadForPartialTransitions() throws Exception {
        final int parallelism = 3;
        final int blockSize = 1000;
        final int jobsForOneRequest = 1;
        String requestFileName = "20mz_DIA_100k.ChorusRequest.xml";
        final int[] sourceFileIds = {
                39879 //Q_2014_0523_115_0_amol_uL_20mz.raw
        };

        final List<String> subRequests = splitFileIntoBlocks(requestFileName, blockSize);

        final long startTime = System.currentTimeMillis();
        final List<JobResult> jobResultList = runSkylineLoadForPartialTransitions(parallelism, jobsForOneRequest, subRequests, sourceFileIds);
        final long endTime = System.currentTimeMillis();
        final int jobQuantity = jobResultList.size();
        LOGGER.info("\n\nThere are " + jobQuantity + " requests are completed in " + (endTime - startTime) + " ms.");
        checkAndLogJobResults(jobResultList);
    }

    @Test(enabled = false)
    public void testSkylineLoadForPartialTransitionsMultiply() throws Exception {
        int virtualUserCount = 20;
        final int parallelism = 3;
        final int blockSize = 1000;
        final int jobsForOneRequest = 1;
        String fileName = "20mz_DIA_100k.ChorusRequest.xml";
        final int[] sourceFileIds = {
                39879 //Q_2014_0523_115_0_amol_uL_20mz.raw
        };

        final List<String> subRequests = splitFileIntoBlocks(fileName, blockSize);
        final List<Callable<List<JobResult>>> jobs = new ArrayList<>(virtualUserCount);
        for (int i = 0; i < virtualUserCount; i++) {
            jobs.add(() -> {
                        return runSkylineLoadForPartialTransitions(parallelism, jobsForOneRequest, subRequests, sourceFileIds);
                    }
            );
        }

        final ExecutorService executorService = Executors.newFixedThreadPool(virtualUserCount);
        final long startTime = System.currentTimeMillis();
        final List<Future<List<JobResult>>> results = executorService.invokeAll(jobs);
        final long endTime = System.currentTimeMillis();
        executorService.shutdownNow();

        final List<JobResult> jobResultList = new ArrayList<>();
        for (Future<List<JobResult>> jobResultFuture: results){
            final List<JobResult> jobResultSubList = jobResultFuture.get();
            jobResultList.addAll(jobResultSubList);
        }

        final int jobQuantity = jobResultList.size();
        LOGGER.info("\n\nThere are " + jobQuantity + " requests are completed in " + (endTime - startTime) + " ms.");
        checkAndLogJobResults(jobResultList);
    }

    @Test(enabled = false)
    public void testTobiasDataFile() throws  InterruptedException {
        final String sampleRequest = SkylineTestUtils.readRequestFromFile("9-sep-tobias.xml");
        hitExtractor(sampleRequest,
                new int[]{79728},     //also reproduced with 80394
                1, 1);
    }

    private static void extractSingleSpectrum(int fileId, String source, String precursor, int sourceRt) throws IOException {
        extractSingleSpectrum(fileId, source, precursor, sourceRt, BASE_URL);
    }

    private static void extractSingleSpectrum(int fileId, String source, String precursor, int sourceRt, String baseUrl) throws IOException {
        //URL pattern is /chroextract/file/{fileId}/source/{chroSource}/precursor/{precursor}/{scanIndex}

        final int rt = sourceRt * (int) MzConversion.INT;
        final String contentPath = "chroextract/file/" + fileId + "/source/" + source + "/precursor/" + precursor + "/" + rt;
        final String uri = baseUrl + contentPath;
        LOGGER.info("Querying the URL: " + uri);
        final HttpGet request = new HttpGet(uri);

        final HttpClient client = HttpClientBuilder.create().build();

        final String authenticationString = getBasicAuthenticationString("pavel.kaplin@gmail.com", "pwd");
        request.setHeader(AUTH_HEADER, authenticationString);
        final long requestStart = System.currentTimeMillis();
        final HttpResponse response = client.execute(request);
        final long requestEnd = System.currentTimeMillis();
        final StatusLine statusLine = response.getStatusLine();

        final int statusCode = statusLine.getStatusCode();

        final long time = requestEnd - requestStart;
        LOGGER.info("Single spectrum extraction for file ID = " + fileId + " completed in " + time + "ms. Status: " + statusCode);
        final List list = IOUtils.readLines(response.getEntity().getContent());
        LOGGER.info("Response: ");
        for (Object o : list) {
            LOGGER.info(" > " + o);
        }
        Assert.assertEquals("Request failed", 200, statusCode);
    }

    private static void extractSingleSpectrumWithDriftTime(int fileId, String source, String precursor, int sourceRt, double driftTime) throws IOException {
        //URL pattern is "/chroextract-drift/file/{fileId}/source/{chroSource}/precursor/{precursor}/{scanIndex}/drift/{driftTime}"


        final int rt = sourceRt * (int) MzConversion.INT;
        final String contentPath = "chroextract-drift/file/" + fileId + "/source/" + source + "/precursor/" + precursor + "/" + rt +"/drift/" + driftTime;
        final String uri = BASE_URL + contentPath;
        LOGGER.info("Querying the URL: " + uri);
        final HttpGet request = new HttpGet(uri);

        final HttpClient client = HttpClientBuilder.create().build();

        final String authenticationString = getBasicAuthenticationString("pavel.kaplin@gmail.com", "pwd");
        request.setHeader(AUTH_HEADER, authenticationString);
        final long requestStart = System.currentTimeMillis();
        final HttpResponse response = client.execute(request);
        final long requestEnd = System.currentTimeMillis();
        final StatusLine statusLine = response.getStatusLine();

        final int statusCode = statusLine.getStatusCode();
        final long time = requestEnd - requestStart;
        LOGGER.info("Single spectrum extraction for file ID = " + fileId + " completed in " + time + "ms. Status: " + statusCode);
        final List list = IOUtils.readLines(response.getEntity().getContent());
        LOGGER.info("Response: ");
        for (Object o : list) {
            LOGGER.info(" > " + o);
        }
    }

    private static class JobResult {
        long time;
        boolean failed = false;
        String failedJobInfo;

        public JobResult(long time) {
            this.time = time;
        }

        public JobResult(long time, boolean failed, String failedJobInfo) {
            this.time = time;
            this.failed = failed;
            this.failedJobInfo = failedJobInfo;
        }

        public long getTime() {
            return time;
        }

        public boolean isFailed() {
            return failed;
        }

        public String getFailedJobInfo() {
            return failedJobInfo;
        }
    }

    private static List<JobResult> hitExtractor(final String sampleRequest, int[] fileIds, int jobCount, int parallelism) throws InterruptedException {
        return hitExtractor(sampleRequest, fileIds, jobCount, parallelism, null, BASE_URL);
    }

    private static List<JobResult> hitExtractor(final String sampleRequest, int[] fileIds, int jobCount, int parallelism, Integer requestBlockNumber, final String baseUrl) throws InterruptedException {
        final ExecutorService executorService = Executors.newFixedThreadPool(parallelism);
        final List<Callable<JobResult>> jobs = new ArrayList<>(jobCount);

        for (int jobIdx = 0; jobIdx < jobCount; jobIdx++) {
            for (final int fileId : fileIds) {
                jobs.add(new Callable<JobResult>() {
                    @Override
                    public JobResult call() throws Exception {


                        final HttpClient client = HttpClientBuilder.create().build();
                        final String contentPath = "chroextract/file/" + fileId;
                        final HttpPost request = new HttpPost(baseUrl + contentPath);
                        final HttpEntity requestEntity = new ByteArrayEntity(sampleRequest.getBytes("UTF-8"));
                        request.setEntity(requestEntity);

                        final String authenticationString = getBasicAuthenticationString("pavel.kaplin@gmail.com", "pwd");
                        request.setHeader(AUTH_HEADER, authenticationString);
                        final long requestStart = System.currentTimeMillis();
                        final HttpResponse response = client.execute(request);
                        final long requestEnd = System.currentTimeMillis();
                        final StatusLine statusLine = response.getStatusLine();
                        final int statusCode = statusLine.getStatusCode();
                        final InputStream is = response.getEntity().getContent();
                        final int availableSize = is.available();
                        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        final long time = requestEnd - requestStart;
                        final String blockNumberStr = requestBlockNumber != null ? " (block " + requestBlockNumber + ")": "";
                        final String jobInfo = "Request for file ID = " + fileId + blockNumberStr + " completed in " + time + "ms. Status: " + statusCode;
                        final JobResult jobResult;
                        if(statusCode == 200) {
                            LOGGER.info( jobInfo );
                            jobResult = new SkylineProductionIntegrationTest.JobResult(time);
                        } else {
                            IOUtils.copy(is, byteArrayOutputStream);
                            final String errorMessage = new String(byteArrayOutputStream.toByteArray());
                            final String failedJobInfo = jobInfo + "\n" + "***  Error message is " + errorMessage;
                            LOGGER.error(failedJobInfo);
                            jobResult = new SkylineProductionIntegrationTest.JobResult(time, true, failedJobInfo);
                        }
                        return jobResult;
                    }
                });
            }
        }

        final long startInvocation = System.currentTimeMillis();
        final List<Future<JobResult>> results = executorService.invokeAll(jobs);
        final long endInvocation = System.currentTimeMillis();

        executorService.shutdownNow();

        LOGGER.info(" Invocation of " + jobCount + " took " + (endInvocation - startInvocation) + "ms.");

        final List<Long> requestTimes = new ArrayList<>();
        final List<JobResult> jobResults = new ArrayList<>();

        long sum = 0;
        for (Future<JobResult> jobResultFeature  : results) {
            JobResult jobResult;
            try {
                jobResult = jobResultFeature.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            long requestTime = jobResult.getTime();
            requestTimes.add(requestTime);
            jobResults.add(jobResult);
            sum += requestTime;
        }
        final Long maxResponseTime = Collections.max(requestTimes);
        final Long minResponseTime = Collections.min(requestTimes);

        LOGGER.info(" > Mean time: " + ((double) sum / requestTimes.size()) +
                        "\n  > Max time: " + maxResponseTime + "" +
                        "\n  > Min time: " + minResponseTime
        );
        Thread.sleep(1000);
        return jobResults;
    }

    private static String getBasicAuthenticationString(String username, String password) {
        final String combined = username + ":" + password;
        final byte[] encoded = Base64.encodeBase64(combined.getBytes());
        return "Basic " + new String(encoded);
    }
}
