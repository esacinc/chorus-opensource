package com.infoclinika.integration.skyline;

import com.google.common.collect.Sets;
import com.infoclinika.analysis.storage.cloud.CloudStorageItemReference;
import com.infoclinika.chorus.integration.skyline.api.SingleSpectrumExtractionRequest;
import com.infoclinika.msdata.image.MSRect;
import com.infoclinika.msdata.image.MSSpectrum;
import com.infoclinika.msdata.image.MzConversion;
import com.infoclinika.msdata.image.SpectrumType;
import computations.MessagingComputationsClient;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Set;

/**
 * @author Oleksii Tymchenko
 */
public class LazySingleSpectrumExtractionJob extends AbstractLazyExtractionJob {
    private static final Logger LOGGER = Logger.getLogger(LazySingleSpectrumExtractionJob.class);

    private final SingleSpectrumExtractionRequest request;

    public LazySingleSpectrumExtractionJob(MessagingComputationsClient computationsClient,
                                           Set<CloudStorageItemReference> ms1Contents,
                                           Set<CloudStorageItemReference> ms2Contents,
                                           Set<CloudStorageItemReference> simContents,
                                           SingleSpectrumExtractionRequest request) {
        super(computationsClient, ms1Contents, simContents, ms2Contents, 1);
        this.request = request;
    }

    @Override
    public LazyExtractionResult call() {
        final long start = System.currentTimeMillis();
        CloudStorageItemReference matchingFilter;
        switch (request.chromSource) {
            case MS_1:
                matchingFilter = pickMatchingFilter(ms1TranslatedContent, simTranslatedContent, request.precursor);
                break;
            case MS_2:
                matchingFilter = pickMatchingMS2Filter(ms2TranslatedContent, request.precursor); //TODO:2016-04-06:andrii.loboda: move all of these methods to service
                break;
            case SIM:
                matchingFilter = pickMatchingFilter(Sets.<CloudStorageItemReference>newHashSet(), simTranslatedContent, request.precursor);
                break;
            default:
                throw new ChroExtractionValidationException("Unknown source type: " + request.chromSource);
        }
        //we are using RT for identification
        final int rtConverted = request.scanID;
        final double rtOriginal = (double) rtConverted / MzConversion.INT;

        //no mz error requested for single spectra extraction requests
        final LazyUnpacker unpacker = lazyExtractFromStorm(matchingFilter, SpectrumType.TIC_SPECTRUM, new MSRect(rtConverted, rtConverted, -1, -1), null, null);
        final long end = System.currentTimeMillis();
        LOGGER.info(" [" + (end - start) + " ms] Single RAW result prepared for request: " + request);
        return new LazyExtractionResult() {
            @Override
            public ExtractionResult get() {
                final long start = System.currentTimeMillis();
                final List<MSSpectrumExtractedItem> spectrums = unpacker.unpack();
                final MSSpectrumExtractedItem spectrum = spectrums.get(0);
                final SingleSpectrumExtractionResult extractionResult = convertToSafeResult(rtConverted, rtOriginal, spectrum.spectrum);
                final long end = System.currentTimeMillis();
                LOGGER.info(" [" + (end - start) + " ms] Single Result TRANSFORMED");
                return new ExtractionResult(order, extractionResult);
            }
        };
    }

    private SingleSpectrumExtractionResult convertToSafeResult(int rtConverted, double rtOriginal, MSSpectrum spectrum) {
        SingleSpectrumExtractionResult result;
        if (spectrum == null) {
            result = new SingleSpectrumExtractionResult(rtConverted, rtOriginal, new double[]{}, new float[]{});
        } else {
            final int[] convertedMzs = spectrum.getMzs();
            double[] originalMzs = new double[convertedMzs.length];
            for (int i = 0; i < convertedMzs.length; i++) {
                int convertedMz = convertedMzs[i];
                originalMzs[i] = (double) convertedMz / MzConversion.INT;
            }
            final float[] intensities = spectrum.getIntensities();
            result = new SingleSpectrumExtractionResult(rtConverted, rtOriginal, originalMzs, intensities);
        }
        return result;
    }
}
