package com.infoclinika.integration.skyline;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.infoclinika.analysis.storage.cloud.CloudStorageItemReference;
import com.infoclinika.msdata.image.MSRect;
import com.infoclinika.msdata.image.MSSpectrum;
import com.infoclinika.msdata.image.MzConversion;
import com.infoclinika.msdata.image.SpectrumType;
import com.infoclinika.mssharing.model.extraction.exception.NoMatchingFilterForPrecursorException;
import com.infoclinika.tasks.api.workflow.ChromatogramSource;
import com.infoclinika.tasks.api.workflow.SpectrumResponseItem;
import com.infoclinika.tasks.api.workflow.SubrangeDescription;
import com.infoclinika.tasks.api.workflow.input.ChroExtractionTask;
import com.infoclinika.tasks.api.workflow.output.ChroExtractionTaskResult;
import com.infoclinika.tasks.api.workflow.output.RawChroExtractionResult;
import computations.MessagingComputationsClient;
import computations.impl.MessagingSerialization;
import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.ChromatogramGroupPoints;
import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.GroupPoints;
import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.chromatogramrequest.ChromExtractor;
import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.chromatogramrequest.ChromSource;
import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.chromatogramrequest.ChromatogramRequestDocument;
import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.chromatogramrequest.ChromatogramRequestDocument.ChromatogramGroup.Chromatogram;
import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.chromatogramrequest.Ms2FullScanAcquisitionMethod;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

/**
 * @author Oleksii Tymchenko
 */
public class LazyRequestResultExtractionJob extends AbstractLazyExtractionJob {
    private static final Logger LOGGER = Logger.getLogger(LazyRequestResultExtractionJob.class);
    public static final int GROUP_PARALLELISM_FOR_SUBRANGES = 5;
    public static final int MAX_GROUP_PROCESSING_PARALLELISM_IMS = 50;

    private final ChromatogramRequestDocument.ChromatogramGroup group;
    private final ChromatogramRequestDocument chromatogramRequestDocument;
    private int groupProcessingParallelism;

    LazyRequestResultExtractionJob(int order,
                                   MessagingComputationsClient computationsClient,
                                   ChromatogramRequestDocument.ChromatogramGroup group,
                                   Set<CloudStorageItemReference> ms1Contents,
                                   Set<CloudStorageItemReference> ms2Contents,
                                   Set<CloudStorageItemReference> simContents,
                                   ChromatogramRequestDocument chromatogramRequestDocument) {
        super(computationsClient, ms1Contents, simContents, ms2Contents, order);
        this.group = group;
        this.chromatogramRequestDocument = chromatogramRequestDocument;
        this.groupProcessingParallelism = GROUP_PARALLELISM_FOR_SUBRANGES;
    }

    @Override
    public LazyExtractionResult call() throws Exception {
        if (group.getDriftTime() == null) {
            if (ExtractionContentExpert.getDriftFnCount(ms1TranslatedContent, ms2TranslatedContent) > 0) {
                //use non-drift-time fns and process as non-IMS data
                final CloudStorageItemReference globalMS1Fn = ExtractionContentExpert.findPrecalculatedContent(ms1TranslatedContent, ExtractionContentExpert.GLOBAL_FN_PREFIX_MS1);
                final CloudStorageItemReference globalMS2Fn = ExtractionContentExpert.findPrecalculatedContent(ms2TranslatedContent, ExtractionContentExpert.GLOBAL_FN_PREFIX_MS2);
                if (globalMS1Fn != null && globalMS2Fn != null) {
                    LOGGER.info("IMS data has precalculated global MS1 and MS2 fns. Drift time is not specified in the request. Continuing the processing as non-IMS data.");
                    ms1TranslatedContent.clear();
                    ms2TranslatedContent.clear();
                    ms1TranslatedContent.add(globalMS1Fn);
                    ms2TranslatedContent.add(globalMS2Fn);

                    simTranslatedContent.clear();
                }
            }

            return processNonImsData();
        } else {

            int totalDriftFns = ExtractionContentExpert.getDriftFnCount(ms1TranslatedContent, ms2TranslatedContent);
            if (totalDriftFns == 0) {
                return processNonImsData();
            } else {
                this.groupProcessingParallelism = Integer.min(MAX_GROUP_PROCESSING_PARALLELISM_IMS, totalDriftFns);
                LOGGER.debug("IMS data group processing parallelism has been set to " + this.groupProcessingParallelism);
                return processIonMobilityData();
            }
        }
    }

    private LazyExtractionResult processNonImsData() throws IOException {
        boolean isCEData = ExtractionContentExpert.detectCollisionEnergyData(ms2TranslatedContent);

        final ChromSource source = group.getSource();
        GroupPointsTransformer transformer;
        final long start = System.currentTimeMillis();

        switch (source) {
            case MS_1:
                if (group.getPrecursorMz() == 0) {
                    transformer = processWholeRangesForAllMS1(ms1TranslatedContent, group);
                } else {
                    try {
                        final CloudStorageItemReference matchingMS1 = pickMatchingFromSimAndMS1(ms1TranslatedContent, simTranslatedContent, group);
                        transformer = processMS1Group(matchingMS1, group);
                    } catch (NoMatchingFilterForPrecursorException e) {
                        // according to the new requirements, we should just return an empty result.
                        transformer = emptyTransformer(group);
                    }
                }
                break;
            case MS_2:
                checkEligibleForMs2Extraction(ms2TranslatedContent, group, chromatogramRequestDocument);
                transformer = filterMatchedAndProcessMS2DiaGroup(ms2TranslatedContent, group, isCEData);
                break;
            case SIM:
                try {
                    final CloudStorageItemReference matchingMS1 = pickMatchingFromSimAndMS1(Sets.<CloudStorageItemReference>newHashSet(), simTranslatedContent, group);
                    transformer = processMS1Group(matchingMS1, group);
                } catch (NoMatchingFilterForPrecursorException e) {
                    transformer = emptyTransformer(group);
                }
                break;
            default:
                throw new ChroExtractionValidationException("Unknown source type: " + source);
        }
        final long end = System.currentTimeMillis();
        LOGGER.info(" [" + (end - start) + " ms] RAW result prepared for " + source + " at precursor MZ " + group.getPrecursorMz());

        final GroupPointsTransformer matchedTransformer = transformer;
        return new LazyExtractionResult() {
            @Override
            public ExtractionResult get() {
                final long start = System.currentTimeMillis();
                final GroupPoints groupPoints = matchedTransformer.transformAndGet();
                final long end = System.currentTimeMillis();
                LOGGER.info(" [" + (end - start) + " ms] Result TRANSFORMED for " + source + " at precursor MZ " + group.getPrecursorMz() + ": " + groupPoints.getPointCount() + " points.");
                return new ExtractionResult(order, groupPoints);
            }
        };
    }

    private LazyExtractionResult processIonMobilityData() throws IOException {
        final ChromSource source = group.getSource();
        GroupPointsTransformer transformer;
        final long start = System.currentTimeMillis();

        switch (source) {
            case MS_1:
                if (group.getPrecursorMz() == 0) {
                    if (group.getDriftTime() == null) {
                        //find global precalculated MS1 function and use it for extraction
                        final CloudStorageItemReference globalPrecalculatedMs1 = ExtractionContentExpert.findPrecalculatedContent(ms1TranslatedContent, ExtractionContentExpert.GLOBAL_FN_PREFIX_MS1);
                        transformer = processWholeRangesForAllMS1(newHashSet(globalPrecalculatedMs1), group);
                    } else {
                        //filter out matching drift time MS1 filters and sum the results of their extraction
                        final Set<CloudStorageItemReference> matchingMs1 = matchByDriftTime(ms1TranslatedContent, group.getDriftTime(), group.getDriftTimeWindow());
                        if (matchingMs1 == null || matchingMs1.isEmpty()) {
                            transformer = emptyTransformer(group);
                        } else {
                            transformer = processWholeRangesForAllMS1(matchingMs1, group);
                        }
                    }
                } else {
                    if (group.getDriftTime() != null) {
                        final Set<CloudStorageItemReference> allMsFilters = new HashSet<>();
                        allMsFilters.addAll(ms1TranslatedContent);
                        allMsFilters.addAll(simTranslatedContent);
                        final Set<CloudStorageItemReference> matchingMS1 = matchByDriftTime(allMsFilters, group.getDriftTime(), group.getDriftTimeWindow());
                        if (matchingMS1 == null || matchingMS1.isEmpty()) {
                            transformer = emptyTransformer(group);
                        } else {
                            transformer = processMultipleMs1Groups(newArrayList(matchingMS1), group);
                        }
                    } else {
                        final CloudStorageItemReference globalPrecalculatedMs1 = ExtractionContentExpert.findPrecalculatedContent(ms1TranslatedContent, ExtractionContentExpert.GLOBAL_FN_PREFIX_MS1);
                        transformer = processMS1Group(globalPrecalculatedMs1, group);
                    }
                }
                break;
            case MS_2:
                if (group.getDriftTime() == null) {
                    final CloudStorageItemReference ms2GlobalFn = ExtractionContentExpert.findPrecalculatedContent(ms2TranslatedContent, ExtractionContentExpert.GLOBAL_FN_PREFIX_MS2);
                    final Set<CloudStorageItemReference> translatedContents = newHashSet(ms2GlobalFn);
                    checkEligibleForMs2Extraction(translatedContents, group, chromatogramRequestDocument);
                    transformer = filterMatchedAndProcessMS2DiaGroup(translatedContents, group, false);
                } else {
                    final Set<CloudStorageItemReference> matchingMS2 = matchByDriftTime(ms2TranslatedContent, group.getDriftTime(), group.getDriftTimeWindow());
                    checkEligibleForMs2Extraction(matchingMS2, group, chromatogramRequestDocument);
                    transformer = processMs2GroupForMatchedFilters(group, matchingMS2, SpectrumCollapsingStrategy.SUM);
                }
                break;
            case SIM:
                final Set<CloudStorageItemReference> matchingSim = matchByDriftTime(simTranslatedContent, group.getDriftTime(), group.getDriftTimeWindow());
                transformer = processMultipleMs1Groups(newArrayList(matchingSim), group);
                break;
            default:
                throw new ChroExtractionValidationException("Unknown source type: " + source);
        }
        final long end = System.currentTimeMillis();
        LOGGER.info(" [" + (end - start) + " ms] Ion Mobility RAW result prepared for " + source + " at precursor MZ " + group.getPrecursorMz());

        return new LazyExtractionResult() {
            @Override
            public ExtractionResult get() {
                final long start = System.currentTimeMillis();
                final GroupPoints groupPoints = transformer.transformAndGet();
                final long end = System.currentTimeMillis();
                LOGGER.info(" [" + (end - start) + " ms] Ion Mobility Extraction result TRANSFORMED for " + source + " at precursor MZ " + group.getPrecursorMz() + ": " + groupPoints.getPointCount() + " points.");
                return new ExtractionResult(order, groupPoints);
            }
        };
    }

    private static GroupPointsTransformer emptyTransformer(final ChromatogramRequestDocument.ChromatogramGroup source) {
        LOGGER.warn("No matching filter for the precursor: " + source.getPrecursorMz()
                + ". Source = " + source.getSource()
                + ". Drift time = " + source.getDriftTime() + ", drift time window = " + source.getDriftTimeWindow()
                + ". Returning an empty result...");
        return new GroupPointsTransformer() {
            @Override
            public GroupPoints transformAndGet() {
                return new ChromatogramGroupPoints(source);
            }
        };
    }

    private static Set<CloudStorageItemReference> matchByDriftTime(Set<CloudStorageItemReference> translatedFilterRefs, Double driftTime, Double driftTimeWindow) {
        if (driftTime == null) {
            return translatedFilterRefs;
        }
        final double halfWindow = (driftTimeWindow == null ? 0 : driftTimeWindow) / 2;
        double minDriftTime = driftTime - halfWindow;
        double maxDriftTime = driftTime + halfWindow;
        Set<CloudStorageItemReference> matchedRefs = new HashSet<>();
        for (CloudStorageItemReference translatedFilterRef : translatedFilterRefs) {
            Double driftTimeForFilter = ExtractionContentExpert.parseDriftTime(translatedFilterRef);
            if (driftTimeForFilter != null && driftTimeForFilter >= minDriftTime && driftTimeForFilter <= maxDriftTime) {
                matchedRefs.add(translatedFilterRef);
            }
        }
        return matchedRefs;
    }


    private GroupPointsTransformer processWholeRangesForAllMS1(
            final Set<CloudStorageItemReference> ms1TranslatedContent,
            final ChromatogramRequestDocument.ChromatogramGroup group) {
        LOGGER.debug("Gathering the whole ranges from all MS1 functions for precursor: " + group.getPrecursorMz());

        final MSSpectrum.SpectrumAppendFunction spectrumAppendFunction = MSSpectrum.appendFunctionFor(toSpectrumType(group.getExtractor()));


        final List<ExtractedItemsValidator> validators = new ArrayList<>();
        for (CloudStorageItemReference ms1Filter : ms1TranslatedContent) {
            final ExtractedItemsValidator validator = extractMS1Spectrums(ms1Filter, group);
            validators.add(validator);
        }
        return new GroupPointsTransformer() {
            @Override
            public GroupPoints transformAndGet() {
                MSSpectrum resultingSpectrum = null;
                for (ExtractedItemsValidator validator : validators) {
                    final List<MSSpectrumExtractedItem> extractedItems = validator.validateAndGet();
                    if (extractedItems.size() != 1) {
                        throw new IllegalStateException("Expected only one spectrum for filter, but got " + (extractedItems.size()) + ": ");
                    }
                    final MSSpectrumExtractedItem spectrumItem = extractedItems.get(0);
                    if (resultingSpectrum == null) {
                        resultingSpectrum = spectrumItem.spectrum;
                    } else {
                        try {
                            spectrumAppendFunction.appendSpectrum(resultingSpectrum, spectrumItem.spectrum);
                        } catch (Exception e) {
                            throw new RuntimeException("Cannot append spectrum for group " + groupToString(group)
                                    + ". Translated contents: " + Arrays.toString(ms1TranslatedContent.toArray()), e);
                        }
                    }
                }
                return transformToGroup(group, newArrayList(new MSSpectrumExtractedItem(resultingSpectrum, null)));
            }
        };

    }

    private CloudStorageItemReference pickMatchingFromSimAndMS1(Set<CloudStorageItemReference> ms1TranslatedContent,
                                                                Set<CloudStorageItemReference> simTranslatedContent,
                                                                ChromatogramRequestDocument.ChromatogramGroup group) throws NoMatchingFilterForPrecursorException {
        return pickMatchingFilter(ms1TranslatedContent, simTranslatedContent, group.getPrecursorMz());
    }

    private static void checkEligibleForMs2Extraction(Set<CloudStorageItemReference> translatedContent, ChromatogramRequestDocument.ChromatogramGroup group, ChromatogramRequestDocument chromatogramRequestDocument) {
        if (!chromatogramRequestDocument.getMs2FullScanAcquisitionMethod().equals(Ms2FullScanAcquisitionMethod.DIA)) {
            throw new ChroExtractionValidationException("Non-DIA MS2 data is not supported. Group precursor: " + group.getPrecursorMz() + " \nSource references: \n" + Arrays.toString(translatedContent.toArray()));
        }
    }

    private GroupPointsTransformer filterMatchedAndProcessMS2DiaGroup(final Set<CloudStorageItemReference> translatedContents,
                                                                      final ChromatogramRequestDocument.ChromatogramGroup group,
                                                                      final boolean isCEData) throws IOException {
        if (isCEData) {
            LOGGER.debug(" ^^ Processing MS2 CE-supplied data group. ");
            return processMs2GroupForMatchedFilters(group, translatedContents, SpectrumCollapsingStrategy.AVG);

        } else {
            GroupPointsTransformer groupPointsTransformer = null;
            try {
                final CloudStorageItemReference matchedFilter = pickMatchingMS2Filter(group, translatedContents);
                LOGGER.debug(" ^^ Processing MS2 DIA group. Matched filter: " + matchedFilter.asDelimitedPath());
                groupPointsTransformer = processMs2GroupForMatchedFilters(group, newHashSet(matchedFilter), SpectrumCollapsingStrategy.SUM);
            } catch (NoMatchingFilterForPrecursorException e) {
                //should not happen since we pick the closest filter anyway.
                //but just in case
                groupPointsTransformer = emptyTransformer(group);
            }
            return groupPointsTransformer;
        }

    }

    private GroupPointsTransformer processMs2GroupForMatchedFilters(final ChromatogramRequestDocument.ChromatogramGroup group,
                                                                    final Set<CloudStorageItemReference> matchedFilters,
                                                                    final SpectrumCollapsingStrategy spectrumCollapsingStrategy) {
        final List<Chromatogram> chroItems = group.getChromatogram();

        final List<Callable<LazyUnpacker>> ms2DiaChroJobs = new ArrayList<>(chroItems.size() * matchedFilters.size());
        for (final CloudStorageItemReference matchedFilter : matchedFilters) {
            for (final Chromatogram currentChro : chroItems) {
                ms2DiaChroJobs.add(new Callable<LazyUnpacker>() {
                    @Override
                    public LazyUnpacker call() throws Exception {
                        return extractDiaChro(group, matchedFilter, currentChro);
                    }
                });
            }
        }

        final ExecutorService executorService = Executors.newFixedThreadPool(groupProcessingParallelism);

        final List<Future<LazyUnpacker>> futures;
        try {
            futures = executorService.invokeAll(ms2DiaChroJobs);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return new GroupPointsTransformer() {
            @Override
            public GroupPoints transformAndGet() {
                final LinkedHashMap<Chromatogram, List<MSSpectrumExtractedItem>> groupedByChro = new LinkedHashMap<>();
                for (Chromatogram sources : group.getChromatogram()) {
                    groupedByChro.put(sources, new LinkedList<MSSpectrumExtractedItem>());
                }

                for (Future<LazyUnpacker> future : futures) {
                    MSSpectrumExtractedItem spectrum;
                    try {
                        final LazyUnpacker lazyUnpacker = future.get();
                        spectrum = lazyUnpacker.unpack().get(0);
                    } catch (Exception e) {
                        LOGGER.warn("Cannot extract one of the " + group.getSource().value() + " DIA chro for the precursor: " + group.getPrecursorMz(), e);
                        spectrum = null;
                    }
                    if (spectrum != null) {
                        if (groupedByChro.get(spectrum.chromatogram) == null) {
                            throw new IllegalStateException("Cannot find matching Chromatogram for the extracted; missing value is " + spectrum.chromatogram);
                        }
                        groupedByChro.get(spectrum.chromatogram).add(spectrum);
                    }

                }
                executorService.shutdownNow();

                final List<MSSpectrumExtractedItem> collapsedSpectrums = collapseSpectrums(groupedByChro, spectrumCollapsingStrategy, group);

                if (collapsedSpectrums.isEmpty()) {
                    LOGGER.warn("Spectrums for the group are empty. Group precursor: " + group.getPrecursorMz());
                }
                return transformToGroup(group, collapsedSpectrums);
            }
        };
    }

    private List<MSSpectrumExtractedItem> collapseSpectrums(LinkedHashMap<Chromatogram, List<MSSpectrumExtractedItem>> groupedByChro,
                                                            SpectrumCollapsingStrategy spectrumCollapsingStrategy,
                                                            ChromatogramRequestDocument.ChromatogramGroup group) {
        final List<MSSpectrumExtractedItem> collapsedSpectrums = new LinkedList<>();
        if (SpectrumCollapsingStrategy.SUM.equals(spectrumCollapsingStrategy)) {

            final MSSpectrum.SpectrumAppendFunction spectrumAppendFunction = MSSpectrum.appendFunctionFor(toSpectrumType(group.getExtractor()));

            for (Chromatogram chromatogram : groupedByChro.keySet()) {
                final List<MSSpectrumExtractedItem> extractedItems = groupedByChro.get(chromatogram);
                List<float[]> errorsToAvg = new LinkedList<>();
                MSSpectrum resultingSpectrumForChro = null;
                for (MSSpectrumExtractedItem extractedItem : extractedItems) {
                    if (resultingSpectrumForChro == null) {
                        resultingSpectrumForChro = extractedItem.spectrum;
                    } else {
                        spectrumAppendFunction.appendSpectrum(resultingSpectrumForChro, extractedItem.spectrum);
                    }
                    errorsToAvg.add(extractedItem.mzErrors);
                }
                final float[] avgErrors = weightedAvgErrorsFrom(errorsToAvg);
                collapsedSpectrums.add(new MSSpectrumExtractedItem(resultingSpectrumForChro, avgErrors, chromatogram));
            }
        } else if (SpectrumCollapsingStrategy.AVG.equals(spectrumCollapsingStrategy)) {
            for (Chromatogram chromatogram : groupedByChro.keySet()) {
                final List<MSSpectrumExtractedItem> extractedItems = groupedByChro.get(chromatogram);
                List<float[]> errorsToAvg = new LinkedList<>();
                List<MSSpectrum> spectrumsToAvg = new LinkedList<>();
                for (MSSpectrumExtractedItem extractedItem : extractedItems) {
                    spectrumsToAvg.add(extractedItem.spectrum);
                    errorsToAvg.add(extractedItem.mzErrors);
                }
                final float[] avgErrors = weightedAvgErrorsFrom(errorsToAvg);
                final MSSpectrum avgSpectrum = avgSpectrumsFrom(spectrumsToAvg);
                collapsedSpectrums.add(new MSSpectrumExtractedItem(avgSpectrum, avgErrors, chromatogram));
            }
        } else {
            throw new IllegalArgumentException("Unknown spectrum collapsing strategy: " + spectrumCollapsingStrategy);
        }
        return collapsedSpectrums;
    }

    private LazyUnpacker extractDiaChro(ChromatogramRequestDocument.ChromatogramGroup group,
                                        CloudStorageItemReference matchedFilter,
                                        Chromatogram currentChro) throws IOException {
        final double productMz = currentChro.getProductMz();
        final MSRect currentRect = getMsRect(group.getMinTime(), group.getMaxTime(),
                productMz, productMz,
                currentChro.getMzWindow(), currentChro.getMzWindow());
        LOGGER.debug(" ^^^^^^^^ Processing chro with product MZ = " + productMz + ". Rect = " + currentRect);
        return lazyExtractFromStorm(matchedFilter, toSpectrumType(group.getExtractor()), currentRect, productMz, currentChro);
    }

    private CloudStorageItemReference pickMatchingMS2Filter(ChromatogramRequestDocument.ChromatogramGroup group, Set<CloudStorageItemReference> translatedContents) {
        final double groupPrecursor = group.getPrecursorMz();
        return pickMatchingMS2Filter(translatedContents, groupPrecursor);
    }

    private GroupPointsTransformer processMS1Group(final CloudStorageItemReference translatedContent,
                                                   final ChromatogramRequestDocument.ChromatogramGroup group) throws IOException {
        final ExtractedItemsValidator validator = extractMS1Spectrums(translatedContent, group);
        return new GroupPointsTransformer() {
            @Override
            public GroupPoints transformAndGet() {
                final List<MSSpectrumExtractedItem> spectrumItems = validator.validateAndGet();
                return transformToGroup(group, spectrumItems);
            }
        };
    }

    private GroupPointsTransformer processMultipleMs1Groups(final List<CloudStorageItemReference> translatedContents,
                                                            final ChromatogramRequestDocument.ChromatogramGroup group) {
        final ExecutorService executorService = Executors.newFixedThreadPool(groupProcessingParallelism);
        final List<Callable<ExtractedItemsValidator>> jobs = new ArrayList<>(translatedContents.size());
        for (final CloudStorageItemReference translatedContent : translatedContents) {
            jobs.add(new Callable<ExtractedItemsValidator>() {
                @Override
                public ExtractedItemsValidator call() throws Exception {
                    return extractMS1Spectrums(translatedContent, group);
                }
            });
        }
        final List<ExtractedItemsValidator> validators;
        try {
            final List<Future<ExtractedItemsValidator>> futures = executorService.invokeAll(jobs);
            validators = Lists.transform(futures, new Function<Future<ExtractedItemsValidator>, ExtractedItemsValidator>() {
                @Override
                public ExtractedItemsValidator apply(Future<ExtractedItemsValidator> validator) {
                    try {
                        return validator.get();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            executorService.shutdownNow();
        } catch (InterruptedException e) {
            throw new RuntimeException("Error processing multiple MS1 drift-time fns for precursor: " + group.getPrecursorMz(), e);
        }

        return new GroupPointsTransformer() {
            @Override
            public GroupPoints transformAndGet() {

                final MSSpectrum.SpectrumAppendFunction appendFn = MSSpectrum.appendFunctionFor(toSpectrumType(group.getExtractor()));

                final int totalSubranges = group.getChromatogram().size();
                final List<MSSpectrum> spectrumsBySubrange = new LinkedList<>();
                final List<List<float[]>> errorsBySubrange = new LinkedList<>();
                for (ExtractedItemsValidator validator : validators) {
                    final List<MSSpectrumExtractedItem> extractedItems = validator.validateAndGet();

                    if (spectrumsBySubrange.isEmpty()) {
                        for (MSSpectrumExtractedItem extractedItem : extractedItems) {
                            spectrumsBySubrange.add(extractedItem.spectrum);
                            final List<float[]> errors = new LinkedList<>();
                            errors.add(extractedItem.mzErrors);
                            errorsBySubrange.add(errors);
                        }
                    } else {
                        if (spectrumsBySubrange.size() != extractedItems.size() || extractedItems.size() != totalSubranges) {
                            throw new IllegalStateException("Sizes do not match while summing subrange spectrums from drift time fns. " +
                                    "Expected: " + totalSubranges + ", but got: " + extractedItems.size());
                        }

                        for (int i = 0; i < extractedItems.size(); i++) {
                            MSSpectrumExtractedItem extractedItem = extractedItems.get(i);
                            if (spectrumsBySubrange.size() < extractedItems.size()) {
                                spectrumsBySubrange.add(extractedItem.spectrum);
                            } else {
                                final MSSpectrum currentSum = spectrumsBySubrange.get(i);
                                appendFn.appendSpectrum(currentSum, extractedItem.spectrum);
                            }

                            if (errorsBySubrange.size() < extractedItems.size()) {
                                final List<float[]> errors = new LinkedList<>();
                                errors.add(extractedItem.mzErrors);
                                errorsBySubrange.add(errors);
                            } else {
                                errorsBySubrange.get(i).add(extractedItem.mzErrors);
                            }
                        }

                    }
                }
                final List<MSSpectrumExtractedItem> summedExtractedItems = new ArrayList<>(totalSubranges);
                for (int i = 0; i < spectrumsBySubrange.size(); i++) {
                    MSSpectrum msSpectrum = spectrumsBySubrange.get(i);
                    final float[] errors = weightedAvgErrorsFrom(errorsBySubrange.get(i));
                    summedExtractedItems.add(new MSSpectrumExtractedItem(msSpectrum, errors));
                }
                return transformToGroup(group, summedExtractedItems);
            }
        };
    }

    private ExtractedItemsValidator extractMS1Spectrums(final CloudStorageItemReference translatedContent,
                                                        final ChromatogramRequestDocument.ChromatogramGroup group) {
        final List<Chromatogram> chroItems = new ArrayList<>(group.getChromatogram());

        Collections.sort(chroItems, new Comparator<Chromatogram>() {
            @Override
            public int compare(Chromatogram o1, Chromatogram o2) {
                return Double.compare(o1.getProductMz(), o2.getProductMz());
            }
        });

        final Double minTime = group.getMinTime();
        final Double maxTime = group.getMaxTime();

        final Chromatogram firstChro = chroItems.get(0);
        final Chromatogram lastChro = chroItems.get(chroItems.size() - 1);
        final double minMz = firstChro.getProductMz();
        final double maxMz = lastChro.getProductMz() + lastChro.getMzWindow();

        final MSRect globalRange = getMsRect(minTime, maxTime, minMz, maxMz, firstChro.getMzWindow(), lastChro.getMzWindow());

        final ChromExtractor extractor = group.getExtractor();
        final SpectrumType spectrumToRender = toSpectrumType(extractor);
        final List<SubrangeDescription> subranges = new ArrayList<>(chroItems.size());

        for (Chromatogram currentChro : chroItems) {
            final double productMz = currentChro.getProductMz();
            final MSRect subrange = getMsRect(minTime, maxTime, productMz, productMz,
                    currentChro.getMzWindow(), currentChro.getMzWindow());
            final SubrangeDescription description = new SubrangeDescription(subrange, productMz);
            subranges.add(description);
        }
        if (subranges.size() == 1 && subranges.iterator().next().getRect().equals(globalRange)) {
            subranges.clear();
        }
        final LazyUnpacker lazyUnpacker = lazyExtractFromStorm(translatedContent, spectrumToRender, globalRange, subranges);

        return new ExtractedItemsValidator() {
            @Override
            public List<MSSpectrumExtractedItem> validateAndGet() {
                final List<MSSpectrumExtractedItem> spectrumItems = lazyUnpacker.unpack();
                if (spectrumItems == null) {
                    throw new IllegalStateException("Spectrums for the group are empty. Group: " + group);
                }
                if (spectrumItems.size() != chroItems.size()) {
                    LOGGER.warn("Spectrums count does not match the expected size. Expected: " + chroItems.size() + ", got: " + spectrumItems.size());
                }
                return spectrumItems;
            }
        };

    }

    private LazyUnpacker lazyExtractFromStorm(final CloudStorageItemReference translatedContent,
                                              final SpectrumType spectrumToRender,
                                              final MSRect range,
                                              final List<SubrangeDescription> subranges) {
        final ChroExtractionTask task = new ChroExtractionTask(range, spectrumToRender);
        final ChromatogramSource source = new ChromatogramSource(translatedContent, 0);
        task.getSources().add(source);
        task.setStartTimeMillisUtc(System.currentTimeMillis());
        if (!range.isWholeRange()) {
            task.getSubranges().addAll(subranges);
        }

        final RawChroExtractionResult rawResult = computationsClient.extractRaw(task);
        return new LazyUnpacker() {

            @Override
            public List<MSSpectrumExtractedItem> unpack() {
                final ChroExtractionTaskResult result = MessagingSerialization.deserialize(rawResult.getSerializedChroExtractionResult(), ChroExtractionTaskResult.class);
                if (!result.getErrors().isEmpty()) {
                    LOGGER.error("Got errors from extractors. Trying to process the remaining content anyway: \n\t" + Arrays.toString(result.getErrors().toArray()));
                }
                final List<MSSpectrumExtractedItem> spectrumsItems = new LinkedList<>();

                final Set<SpectrumResponseItem> rawSpectrumResponseItems = result.getSpectrums();
                if (rawSpectrumResponseItems.iterator().hasNext()) {
                    final SpectrumResponseItem spectrumResponseItem = rawSpectrumResponseItems.iterator().next();
                    if (!task.getSubranges().isEmpty()) {
                        final Map<Integer, MSSpectrum> subrangeSpectrums = spectrumResponseItem.getSubrangeSpectrums();
                        final Map<Integer, float[]> subrangeMzErrors = spectrumResponseItem.getSubrangeMzErrors();
                        for (SubrangeDescription subrange : task.getSubranges()) {
                            final int key = subrange.getRect().hashCode();
                            final MSSpectrum spectrum = subrangeSpectrums.get(key);
                            if (spectrum == null) {
                                LOGGER.warn("No subrange spectrum returned for source " +
                                        translatedContent.asDelimitedPath() + " and subrange: " + subrange);
                            }
                            final float[] mzErrors = subrangeMzErrors.get(subrange.getRect().hashCode());
                            spectrumsItems.add(new MSSpectrumExtractedItem(spectrum, mzErrors));
                        }
                    } else {
                        spectrumsItems.add(new MSSpectrumExtractedItem(spectrumResponseItem.getSpectrum(), null));
                    }
                } else {
                    LOGGER.warn("No spectrum results returned for task: " + task);
                }

                return spectrumsItems;
            }
        };

    }

    private static float[] weightedAvgErrorsFrom(List<float[]> errorsByRanges) {
        if (errorsByRanges == null || errorsByRanges.size() == 0) {
            throw new IllegalStateException("Cannot calculate avg mz errors for an empty array");
        }
        if (errorsByRanges.size() == 1) {
            return errorsByRanges.get(0);
        } else {
            final float[] sums = new float[errorsByRanges.get(0).length];
            for (float[] errorsForSingleRange : errorsByRanges) {
                for (int i = 0; i < errorsForSingleRange.length; i++) {
                    float singleErrorValue = errorsForSingleRange[i];
                    sums[i] += singleErrorValue;
                }
            }
            final int totalRanges = errorsByRanges.size();
            final float[] avgErrors = new float[sums.length];
            for (int i = 0; i < sums.length; i++) {
                float sum = sums[i];
                final float avg = sum / totalRanges;
                avgErrors[i] = avg;
            }
            return avgErrors;
        }
    }

    private static SpectrumType toSpectrumType(ChromExtractor extractor) {
        SpectrumType spectrumType;
        switch (extractor) {
            case BASE_PEAK:
                spectrumType = SpectrumType.BPI_CHROMATOGRAM;
                break;
            case SUMMED:
                spectrumType = SpectrumType.TIC_CHROMATOGRAM;
                break;
            default:
                throw new IllegalArgumentException("Unknown chrom extractor: " + extractor);
        }
        return spectrumType;
    }

    private static MSRect getMsRect(Double minTime, Double maxTime, double firstStartMz, double lastEndMz, double firstWindow, double lastWindow) {
        final int startMz = firstStartMz == 0.0D ? -1 : (int) (MzConversion.INT * (firstStartMz - (firstWindow / 2)));
        final int endMz = lastEndMz == 0.0D ? -1 : (int) (MzConversion.INT * (lastEndMz + (lastWindow / 2)));

        final int startRt = minTime == null ? -1 : (int) (minTime * MzConversion.INT);
        final int endRt = maxTime == null ? -1 : (int) (maxTime * MzConversion.INT);


        return new MSRect(startRt, endRt, startMz, endMz);
    }

    private static String groupToString(ChromatogramRequestDocument.ChromatogramGroup group) {
        return "Group [" +
                " precursor-mz:" + group.getPrecursorMz() + "" +
                ", min-rt:" + group.getMinTime() + "" +
                ", max-rt:" + group.getMaxTime() + "" +
                ", source:" + group.getSource().value() +
                "]";
    }

    private enum SpectrumCollapsingStrategy {
        SUM,
        AVG
    }


}
