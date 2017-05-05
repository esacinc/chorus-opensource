package com.infoclinika.integration.skyline;

import com.infoclinika.analysis.storage.cloud.CloudStorageItemReference;
import com.infoclinika.common.utils.Pair;
import com.infoclinika.msdata.image.MSSpectrum;
import com.infoclinika.msdata.image.MzConversion;
import com.infoclinika.mssharing.extraction.MsFunctionExtractionContentExpertImpl;
import com.infoclinika.mssharing.model.extraction.MsFunctionExtractionContentExpert;
import com.infoclinika.mssharing.model.extraction.exception.NoMatchingFilterForPrecursorException;
import computations.MessagingComputationsClient;
import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.ChromatogramGroupPoints;
import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.ChromatogramPointValue;
import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.chromatogramrequest.ChromatogramRequestDocument;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.Callable;

/**
 * @author Oleksii Tymchenko
 */
public abstract class AbstractExtractionJob<T> implements Callable<T> {
    private static final Logger LOGGER = Logger.getLogger(AbstractExtractionJob.class);
    private static final MsFunctionExtractionContentExpert msFunctionExtractionContentExpert = new MsFunctionExtractionContentExpertImpl();

    protected final MessagingComputationsClient computationsClient;
    protected final Set<CloudStorageItemReference> ms1TranslatedContent;
    protected final Set<CloudStorageItemReference> ms2TranslatedContent;
    protected final Set<CloudStorageItemReference> simTranslatedContent;
    protected final List<CloudStorageItemReference> sortedRefs;
    protected final int order;

    public AbstractExtractionJob(MessagingComputationsClient computationsClient, Set<CloudStorageItemReference> ms1Contents, Set<CloudStorageItemReference> simContents, Set<CloudStorageItemReference> ms2Contents, int order) {
        this.computationsClient = computationsClient;
        this.ms1TranslatedContent = new HashSet<>(ms1Contents);
        this.simTranslatedContent = new HashSet<>(simContents);
        this.ms2TranslatedContent = new HashSet<>(ms2Contents);
        this.sortedRefs = new ArrayList<>(ms1TranslatedContent.size() + ms2TranslatedContent.size() + simTranslatedContent.size());
        this.order = order;

        initSortedRefs();
    }

    private void initSortedRefs() {
        final TreeSet<CloudStorageItemReference> sortedMs1 = new TreeSet<>(ms1TranslatedContent);
        final TreeSet<CloudStorageItemReference> sortedMs2 = new TreeSet<>(ms2TranslatedContent);
        final TreeSet<CloudStorageItemReference> sortedSim = new TreeSet<>(simTranslatedContent);
        sortedRefs.addAll(sortedMs1);
        sortedRefs.addAll(sortedMs2);
        sortedRefs.addAll(sortedSim);
    }


    protected static CloudStorageItemReference pickMatchingFilter(Set<CloudStorageItemReference> ms1TranslatedContent,
                                                                  Set<CloudStorageItemReference> simTranslatedContent,
                                                                  double precursorMz) throws NoMatchingFilterForPrecursorException {
        final Set<CloudStorageItemReference> filtersToPick = new HashSet<>(ms1TranslatedContent.size() + simTranslatedContent.size());
        filtersToPick.addAll(ms1TranslatedContent);
        filtersToPick.addAll(simTranslatedContent);
        if (filtersToPick.size() == 1) {
            return filtersToPick.iterator().next();
        }
        for (CloudStorageItemReference filter : filtersToPick) {
            final Pair<Double, Double> filterMzRange = ExtractionUtils.parseRange(filter);
            if (filterMzRange != null) {
                if (filterMzRange.getFirst() <= precursorMz && precursorMz <= filterMzRange.getSecond()) {
                    return filter;
                }
            }
        }
        throw new NoMatchingFilterForPrecursorException(precursorMz);
    }

    protected static CloudStorageItemReference pickMatchingMS2Filter(Set<CloudStorageItemReference> translatedContents, final double groupPrecursor) {
        return msFunctionExtractionContentExpert.selectMatchingMS2Filter(translatedContents, groupPrecursor);
    }


    public static ChromatogramGroupPoints transformToGroup(ChromatogramRequestDocument.ChromatogramGroup group,
                                                           List<MSSpectrumExtractedItem> spectrums) {
        try {
            group.setMassErrors(group.isMassErrors());
            final ChromatogramGroupPoints result = new ChromatogramGroupPoints(group);
            if (!spectrums.isEmpty()) {
                final MSSpectrum firstSpectrum = spectrums.get(0).spectrum;
                if (firstSpectrum != null) {
                    final int[] commonRts = firstSpectrum.getRts();
                    for (int rtIndex = 0; rtIndex < commonRts.length; rtIndex++) {

                        final int currentRtInt = commonRts[rtIndex];
                        double currentRt = (double) currentRtInt / MzConversion.INT;

                        final List<ChromatogramPointValue> pointValues = new LinkedList<>();
                        for (int i = 0; i < spectrums.size(); i++) {
                            MSSpectrumExtractedItem spectrumItem = spectrums.get(i);
                            if (spectrumItem == null) {
                                pointValues.add(new ChromatogramPointValue(0, 0));
                            } else {
                                final float intensity = spectrumItem.spectrum.getIntensities()[rtIndex];
                                final float mzError = spectrumItem.mzErrors == null ? 0 : spectrumItem.mzErrors[rtIndex];
                                pointValues.add(new ChromatogramPointValue(intensity, (double) mzError));

    //                            if (i == 0) {
    //                                final int ppmCoef = 1000000;
    //                                System.out.println(" **** rt = " + currentRt + " | intensity: " + intensity + " | mass error: " + (mzError * ppmCoef) + " | mz = " + group.getChromatogram().get(i).getProductMz());
    //                            }
                            }
                        }

                        result.addPoint(currentRt, currentRtInt, pointValues);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            final String msgString = "Error transforming spectrums to a group. ChroGroup: [source = " + group.getSource() + ", precursor MZ = " + group.getPrecursorMz() + "]. Spectrum count = " + spectrums.size();
            LOGGER.error(msgString, e);
            throw new RuntimeException(msgString, e);
        }
    }
}
