package com.infoclinika.integration.skyline;

import com.infoclinika.analysis.storage.cloud.CloudStorageItemReference;
import com.infoclinika.msdata.image.MSRect;
import com.infoclinika.msdata.image.MSSpectrum;
import com.infoclinika.msdata.image.SpectrumType;
import com.infoclinika.tasks.api.workflow.ChromatogramSource;
import com.infoclinika.tasks.api.workflow.SpectrumResponseItem;
import com.infoclinika.tasks.api.workflow.input.ChroExtractionTask;
import com.infoclinika.tasks.api.workflow.output.ChroExtractionTaskResult;
import com.infoclinika.tasks.api.workflow.output.RawChroExtractionResult;
import computations.AbstractMessagingComputationsClient;
import computations.MessagingComputationsClient;
import computations.impl.MessagingSerialization;
import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.GroupPoints;
import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.chromatogramrequest.ChromatogramRequestDocument;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @author Oleksii Tymchenko
 */
public abstract class AbstractLazyExtractionJob extends AbstractExtractionJob<LazyExtractionResult> {
    private static final Logger LOGGER = Logger.getLogger(AbstractLazyExtractionJob.class);

    public AbstractLazyExtractionJob(MessagingComputationsClient computationsClient, Set<CloudStorageItemReference> ms1Contents, Set<CloudStorageItemReference> simContents, Set<CloudStorageItemReference> ms2Contents, int order) {
        super(computationsClient, ms1Contents, simContents, ms2Contents, order);
    }

    protected static MSSpectrum avgSpectrumsFrom(List<MSSpectrum> spectrumsToAvg) {
        final MSSpectrum firstSpectrum = spectrumsToAvg.get(0);
        float[] avgIntensities = new float[firstSpectrum.getIntensities().length];

        for (MSSpectrum msSpectrum : spectrumsToAvg) {
            final float[] currentIntensities = msSpectrum.getIntensities();
            if(currentIntensities.length != avgIntensities.length) {
                throw new IllegalStateException("Error averaging spectrums: expected avg length: " + avgIntensities.length + ";" +
                        " actual incoming length: " + currentIntensities.length);
            }
            for(int i = 0; i < avgIntensities.length; i++) {
                avgIntensities[i] += currentIntensities[i];
            }
        }
        final int totalSpectrums = spectrumsToAvg.size();
        for (int i = 0; i < avgIntensities.length; i++) {
            avgIntensities[i] = avgIntensities[i] / totalSpectrums;
        }
        final MSSpectrum avgSpectrum;
        avgSpectrum = new MSSpectrum(
                firstSpectrum.getSpectrumType(),
                firstSpectrum.getRts(),
                firstSpectrum.getMzs(),
                avgIntensities,
                firstSpectrum.getDataType()
        );
        return avgSpectrum;
    }

    protected LazyUnpacker lazyExtractFromStorm(CloudStorageItemReference translatedContent, SpectrumType spectrumToRender,
                                                MSRect msRect, Double targetMz,
                                                final ChromatogramRequestDocument.ChromatogramGroup.Chromatogram source) {
        final List<ChromatogramSource> sources = newArrayList(new ChromatogramSource(translatedContent, 0));
        final ChroExtractionTask request = new ChroExtractionTask(msRect, spectrumToRender);
        request.setTargetMz(targetMz);
        request.getSources().addAll(sources);
        request.setStartTimeMillisUtc(System.currentTimeMillis());
        final RawChroExtractionResult rawResult = computationsClient.extractRaw(request);
        return new LazyUnpacker() {
            @Override
            public List<MSSpectrumExtractedItem> unpack() {
                ChroExtractionTaskResult response;
                try {
                    response = MessagingSerialization.deserialize(rawResult.getSerializedChroExtractionResult(), ChroExtractionTaskResult.class);
                } catch (Exception e) {
                    //fallback strategy in case the deserialized result is in fact a Composite.
                    //todo[tymchenko]: code smell! refactor!
                    response = AbstractMessagingComputationsClient.fromRawCompositeResult(rawResult);
                }
                if (!response.getErrors().isEmpty()) {
                    LOGGER.error("Got errors from extractors for request: " + request + "\n Trying to process the remaining content anyway: \n\t" + Arrays.toString(response.getErrors().toArray()));
                }
                final Set<SpectrumResponseItem> resulingSpectrumItems = response.getSpectrums();
                if (resulingSpectrumItems.size() != 1) {
                    throw new IllegalStateException("Response returned not exactly one spectrum: " + Arrays.toString(resulingSpectrumItems.toArray()) + ". Original request: " + request);
                }
                final SpectrumResponseItem spectrumItem = resulingSpectrumItems.iterator().next();
                final MSSpectrumExtractedItem extractedItem = new MSSpectrumExtractedItem(spectrumItem.getSpectrum(), spectrumItem.getMzErrors(), source);
                return newArrayList(extractedItem);

            }
        };
    }

    protected interface LazyUnpacker {
        List<MSSpectrumExtractedItem> unpack();
    }

    protected interface ExtractedItemsValidator {
        List<MSSpectrumExtractedItem> validateAndGet();
    }

    protected interface GroupPointsTransformer {
        GroupPoints transformAndGet();
    }
}
