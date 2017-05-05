package com.infoclinika.mssharing.skyline.processing;

import com.google.common.io.Files;
import com.infoclinika.chorus.integration.skyline.api.ChromatogramExtractor;
import com.infoclinika.chorus.integration.skyline.api.SingleSpectrumExtractionRequest;
import com.infoclinika.integration.skyline.ChroExtractionValidationException;
import com.infoclinika.integration.skyline.SkylineExtractor;
import com.infoclinika.mssharing.model.extraction.exception.ChroExtractionException;
import com.infoclinika.tasks.api.ManageableMessagingClient;
import com.infoclinika.tasks.api.workflow.input.CompositeChroExtractionTask;
import com.infoclinika.tasks.api.workflow.output.RawChroExtractionResult;
import computations.impl.ChroExtractionClient;
import computations.impl.ComputationsMessagingService;
import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.chromatogramrequest.ChromSource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Wraps the actual extraction calls with the preprocessing
 *
 * @author Oleksii Tymchenko
 */
@Component
public class ExtractionPreProcessor {
    public static final String SKYLINE_EXTRACTOR_PARALLELISM_PROPERTY = "skyline.extractor.parallelism";
    public static final String SKYLINE_PROCESSING_PARALLELISM_PROPERTY = "skyline.processing.parallelism";
    private static final Logger LOGGER = Logger.getLogger(ExtractionPreProcessor.class);
    private static boolean initialized = false;

    private ChromatogramExtractor skylineExtractor;


    //skyline.extraction.queue=production.skyline.extraction.queue by default
    @Value("${skyline.extraction.queue}")
    private String skylineExtractionQueue;
    //skyline.replies.queue=production.skyline.extraction.queue-replies by default
    @Value("${skyline.replies.queue}")
    private String skylineRepliesQueue;
    //skyline.removes.queue=production.skyline.extraction.queue-removes by default
    @Value("${skyline.removes.queue}")
    private String skylineRemovesQueue;

    private static void sendWrapped(ExtractionRequest request, TranslatedFileData translatedFileData, HttpServletResponse response) {
        if (translatedFileData.ms1Refs.isEmpty() && translatedFileData.ms2Refs.isEmpty() && translatedFileData.simRefs.isEmpty()) {
            writeErrorResponse("File is not translated", null, response);
        } else {
            try {
                request.fire();
            } catch (ChroExtractionException validationException) {
                LOGGER.error("Error validating extraction request", validationException);
                writeErrorResponse(validationException.getMessage(), validationException, response);
            } catch (Exception e) {
                LOGGER.error("Error processing extraction request", e);
                writeErrorResponse("Unknown error", e, response);
            }
        }
    }

    private static void writeErrorResponse(String errorMessage, Exception exception, HttpServletResponse response) {
        final StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append("<ChorusErrorResponse>");
        responseBuilder.append("<Message>");
        responseBuilder.append(errorMessage == null ? "" : errorMessage);
        responseBuilder.append("</Message>");
        responseBuilder.append("<StackTrace>");
        responseBuilder.append(exception == null ? "" : ExceptionUtils.getStackTrace(exception));
        responseBuilder.append("</StackTrace>");
        responseBuilder.append("</ChorusErrorResponse>");

        response.setStatus(500);
        try {
            response.getWriter().write(responseBuilder.toString());
            response.flushBuffer();
        } catch (IOException ignored) {
            LOGGER.warn("Could not fill the error stacktrace ", ignored);
        }
    }

    private static ChromSource chromSourceFromString(String chromSourceString) {
        if ("ms1".equalsIgnoreCase(chromSourceString)) {
            return ChromSource.MS_1;
        }
        if ("ms2".equalsIgnoreCase(chromSourceString)) {
            return ChromSource.MS_2;
        }
        if ("sim".equalsIgnoreCase(chromSourceString)) {
            return ChromSource.SIM;
        }
        throw new ChroExtractionValidationException("Unknown chro source: " + chromSourceString);
    }

    private static void copyToResponse(HttpServletResponse response, ByteArrayOutputStream intermediateDest) throws IOException {
        IOUtils.closeQuietly(intermediateDest);
        response.setStatus(HttpServletResponse.SC_OK);

        final byte[] bytes = intermediateDest.toByteArray();
        LOGGER.info("Result size is " + bytes.length);
        response.setContentLength(bytes.length);
        final ServletOutputStream responseOutputStream = response.getOutputStream();
        IOUtils.write(bytes, responseOutputStream);
        responseOutputStream.flush();
    }

    private static int readExtractionParallelismValue() {
        final String parallelismString = System.getProperty(SKYLINE_EXTRACTOR_PARALLELISM_PROPERTY, "" + SkylineExtractor.DEFAULT_EXTRACTION_PARALLELISM);
        int parallelism;
        try {
            parallelism = Integer.parseInt(parallelismString);
        } catch (Exception e) {
            parallelism = SkylineExtractor.DEFAULT_EXTRACTION_PARALLELISM;
        }
        return parallelism;
    }

    private static int readProcessingParallelismValue() {
        final String parallelismString = System.getProperty(SKYLINE_PROCESSING_PARALLELISM_PROPERTY, "" + SkylineExtractor.DEFAULT_PROCESSING_PARALLELISM);
        int parallelism;
        try {
            parallelism = Integer.parseInt(parallelismString);
        } catch (Exception e) {
            parallelism = SkylineExtractor.DEFAULT_PROCESSING_PARALLELISM;
        }
        return parallelism;
    }

    @PostConstruct
    private void initializeExtractors() {

        if (initialized) {
            return;
        }

        initialized = true;

        final int extractionParallelism = readExtractionParallelismValue();
        final int processingParallelism = readProcessingParallelismValue();
        final File tempDir = Files.createTempDir();
        tempDir.deleteOnExit();
        final String sharedStoragePath = tempDir.getAbsolutePath();

        LOGGER.info("Initializing Skyline extraction for queues:" +
                "\n -- [send to] - " + skylineExtractionQueue +
                "\n -- [replies from] - " + skylineRepliesQueue +
                "\n -- [removes to] - " + skylineRemovesQueue +
                ".\n Extraction parallelism: " + extractionParallelism + ". Processing parallelism: " + processingParallelism +
                ".\n Shared storage path: " + sharedStoragePath);
        final ComputationsMessagingService messagingService = new ComputationsMessagingService();
        final ManageableMessagingClient<CompositeChroExtractionTask, RawChroExtractionResult> messagingClient = messagingService.messagingClientForQueue(
                skylineExtractionQueue, skylineRepliesQueue, skylineRemovesQueue);
        final ChroExtractionClient chroExtractionClient = new ChroExtractionClient(messagingClient);

        skylineExtractor = new SkylineExtractor(chroExtractionClient, sharedStoragePath, extractionParallelism, processingParallelism);
    }

    public void sendExtractRequest(final TranslatedFileData translatedFileData,
                                   final String chroSource,
                                   final double precursor,
                                   final int scanIndex,
                                   final HttpServletResponse response) {

        sendWrapped(new ExtractionRequest() {
            @Override
            public void fire() throws Exception {
                processSingleSpectraRequest(translatedFileData, response, chroSource, precursor, scanIndex);
            }
        }, translatedFileData, response);
    }

    public void sendExtractRequestWithDriftTime(final TranslatedFileData translatedFileData,
                                                final String chroSource,
                                                final double precursor,
                                                final int scanIndex,
                                                final Double driftTime,
                                                final HttpServletResponse response) {
        sendWrapped(new ExtractionRequest() {
            @Override
            public void fire() throws Exception {
                processSingleSpectraRequestWithDriftTime(translatedFileData, response, chroSource, precursor, scanIndex, driftTime);
            }
        }, translatedFileData, response);
    }

    public void sendExtractRequest(final TranslatedFileData translatedFileData,
                                   final String xmlRequest,
                                   final HttpServletResponse response) {

        sendWrapped(new ExtractionRequest() {
            @Override
            public void fire() throws Exception {
                processMultipleSpectraRequest(translatedFileData, xmlRequest, response);
            }
        }, translatedFileData, response);
    }

    private void processSingleSpectraRequest(TranslatedFileData translatedFileData,
                                             HttpServletResponse response,
                                             String chroSource,
                                             double precursor,
                                             int scanIndex) throws Exception {
        final ChromSource chromSource = chromSourceFromString(chroSource);
        final SingleSpectrumExtractionRequest request = new SingleSpectrumExtractionRequest(chromSource, precursor, scanIndex);

        final ByteArrayOutputStream intermediateDest = new ByteArrayOutputStream();
        skylineExtractor.processExtractionRequest(request,
                translatedFileData.ms1Refs, translatedFileData.ms2Refs, translatedFileData.simRefs,
                intermediateDest);
        copyToResponse(response, intermediateDest);
    }

    private void processSingleSpectraRequestWithDriftTime(TranslatedFileData translatedFileData,
                                                          HttpServletResponse response,
                                                          String chroSource,
                                                          double precursor,
                                                          int scanIndex,
                                                          Double driftTime) throws Exception {
        final ChromSource chromSource = chromSourceFromString(chroSource);
        final double convertedDriftTime;
        if (driftTime != null) {
            convertedDriftTime = driftTime;
        } else {
            convertedDriftTime = -1;
        }
        final SingleSpectrumExtractionRequest request = new SingleSpectrumExtractionRequest(chromSource, precursor, convertedDriftTime, scanIndex);

        final ByteArrayOutputStream intermediateDest = new ByteArrayOutputStream();
        skylineExtractor.processExtractionRequest(request,
                translatedFileData.ms1Refs, translatedFileData.ms2Refs, translatedFileData.simRefs,
                intermediateDest);
        copyToResponse(response, intermediateDest);
    }

    private void processMultipleSpectraRequest(TranslatedFileData translatedFileData, String xmlRequest, HttpServletResponse response) throws Exception {
        final ByteArrayOutputStream intermediateDest = new ByteArrayOutputStream();
        final long startProcessing = System.currentTimeMillis();
        skylineExtractor.processExtractionRequest(xmlRequest,
                translatedFileData.ms1Refs, translatedFileData.ms2Refs, translatedFileData.simRefs,
                intermediateDest);
        LOGGER.info(" - Pre-processor extraction complete. Time spent: " + (System.currentTimeMillis() - startProcessing) + " ms.");
        response.setContentType("application/json");
        copyToResponse(response, intermediateDest);
    }

    private interface ExtractionRequest {
        void fire() throws Exception;
    }

}
