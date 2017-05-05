package com.infoclinika.mssharing.model.internal.read;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.infoclinika.analysis.storage.cloud.CloudStorageItemReference;
import com.infoclinika.mssharing.model.api.MSFunctionType;
import com.infoclinika.mssharing.model.internal.entity.*;
import com.infoclinika.mssharing.model.internal.entity.restorable.AbstractExperiment;
import com.infoclinika.mssharing.model.internal.entity.restorable.AbstractFileMetaData;
import com.infoclinika.mssharing.model.internal.entity.PrepToExperimentSample;
import com.infoclinika.tasks.api.workflow.model.ExperimentPreparedSampleItem;
import com.infoclinika.tasks.api.workflow.model.ExperimentSampleItem;
import com.infoclinika.tasks.api.workflow.model.ExperimentSampleTypeItem;
import com.infoclinika.tasks.api.workflow.model.MsFunctionFileItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

/**
 * Reads files from persistence with extra information about msfunctions
 *
 * @author andrii.loboda
 */
@Service
@Transactional(readOnly = true)
public class MsFunctionReaderImpl {
    @Value("${amazon.active.bucket}")
    private String targetBucket;

    /*
        *  * Reads raw files, find MS1 and MS2 functions and transforms to {@link com.infoclinika.tasks.api.workflow.model.MsFunctionFileItem}*/
    public Set<MsFunctionFileItem> getMsFunctionFileItems(String msFunctionName, String ms2FunctionName, Set<RawFile> rawFiles) {
        final List<MsFunctionFileItem> fileItems = newLinkedList();
        for (RawFile rawFile : rawFiles) {
            final MsFunctionFileItem fileItem = getMsFunctionItem(rawFile, msFunctionName, ms2FunctionName);
            fileItems.add(fileItem);

        }
        return newHashSet(fileItems);
    }

    public MsFunctionFileItem getMsFunctionItem(final RawFile rawFile, String msFunctionName, String ms2FunctionName) {
        CloudStorageItemReference msReference = null;
        CloudStorageItemReference ms2Reference = null;
        final AbstractFileMetaData fileMetaData = (AbstractFileMetaData) rawFile.getFileMetaData();

        final List<MSFunctionItem> functions = findFileMsFunctions(rawFile, fileMetaData);

        checkState(!functions.isEmpty(), "No found MSFunctions for file meta data: %s", fileMetaData.getId());

        Collections.sort(functions, new Comparator<MSFunctionItem>() {
            @Override
            public int compare(MSFunctionItem o1, MSFunctionItem o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });

        Integer lowMz = null;
        Integer highMz = null;
        int ms2FunctionsCount = 0;
        for (MSFunctionItem ms : functions) {
            if (ms.getFunctionType() == MSFunctionType.MS && msReference == null) {
                if (Strings.isNullOrEmpty(msFunctionName) || ms.getFunctionName().startsWith(msFunctionName)) {
                    msReference = new CloudStorageItemReference(targetBucket, ms.getTranslatedPath());
                    lowMz = ms.getLowMz();
                    highMz = ms.getHighMz();
                }
            }
            if (ms.getFunctionType() == MSFunctionType.MS2) {
                ms2FunctionsCount++;
                if (ms2Reference == null) {
                    if (Strings.isNullOrEmpty(ms2FunctionName) || ms.getFunctionName().startsWith(ms2FunctionName)) {
                        ms2Reference = new CloudStorageItemReference(targetBucket, ms.getTranslatedPath());
                    }
                }
            }
        }
        final CloudStorageItemReference rawFileReference = new CloudStorageItemReference(targetBucket, rawFile.getFileMetaData().getContentId());
        final boolean dia = ms2FunctionsCount > 1;
        return new MsFunctionFileItem(rawFile.getId(), fileMetaData.getName(),
                msReference == null ? null : msReference.asDelimitedPath(),
                ms2Reference == null ? null : ms2Reference.asDelimitedPath(),
                rawFileReference.asDelimitedPath(), ((Number) rawFile.getFractionNumber()).byteValue(),
                asPreparedSample(rawFile.getPreparedSample()), dia, lowMz, highMz);
    }

    private static List<MSFunctionItem> findFileMsFunctions(RawFile rawFile, AbstractFileMetaData fileMetaData) {
        final AbstractExperiment experiment = rawFile.getExperiment();
        Lab lab = experiment.getLab();
        final Lab billLab = experiment.getBillLaboratory();
        if (lab == null) {
            lab = billLab;
        }
        checkNotNull(lab, "Lab should be specified for processing run");
        final List<MSFunctionItem> labFunctions = getMsFunctionItems(lab.getId(), fileMetaData);
        if (!labFunctions.isEmpty()) {
            return labFunctions;
        } else if (billLab != null) {
            final List<MSFunctionItem> billLabFunctions = getMsFunctionItems(billLab.getId(), fileMetaData);
            return billLabFunctions;
        }
        return Collections.emptyList();
    }

    private static ExperimentPreparedSampleItem asPreparedSample(ExperimentPreparedSample preparedSample) {
        final HashSet<ExperimentSampleItem> sampleItems = newHashSet();
        for (PrepToExperimentSample prepToExperimentSample : preparedSample.getSamples()) {
            final ExperimentSample sample = prepToExperimentSample.getExperimentSample();
            final Set<SampleCondition> sampleConditions = sample.getSampleConditions(); // only one condition could be for one sample per raw file(file in experiment)
            final SampleCondition sampleCondition = sampleConditions.isEmpty() ? SampleCondition.createUndefinedCondition(null, ImmutableSet.of(sample)) : sampleConditions.iterator().next();
            sampleItems.add(
                    new ExperimentSampleItem(
                            sample.getId(),
                            sample.getName(),
                            asSampleType(prepToExperimentSample.getType()),
                            sampleCondition.getId(),
                            sampleCondition.getName()
                    )
            );
        }

        return new ExperimentPreparedSampleItem(preparedSample.getId(), preparedSample.getName(), sampleItems);
    }

    private static ExperimentSampleTypeItem asSampleType(ExperimentSampleType type) {
        switch (type) {
            case LIGHT:
                return ExperimentSampleTypeItem.LIGHT;
            case MEDIUM:
                return ExperimentSampleTypeItem.MEDIUM;
            case HEAVY:
                return ExperimentSampleTypeItem.HEAVY;
            case SPECIAL:
                return ExperimentSampleTypeItem.SPECIAL;
            case CHANNEL_1:
                return ExperimentSampleTypeItem.CHANNEL_1;
            case CHANNEL_2:
                return ExperimentSampleTypeItem.CHANNEL_2;
            case CHANNEL_3:
                return ExperimentSampleTypeItem.CHANNEL_3;
            case CHANNEL_4:
                return ExperimentSampleTypeItem.CHANNEL_4;
            case CHANNEL_5:
                return ExperimentSampleTypeItem.CHANNEL_5;
            case CHANNEL_6:
                return ExperimentSampleTypeItem.CHANNEL_6;
            case CHANNEL_7:
                return ExperimentSampleTypeItem.CHANNEL_7;
            case CHANNEL_8:
                return ExperimentSampleTypeItem.CHANNEL_8;
            case CHANNEL_9:
                return ExperimentSampleTypeItem.CHANNEL_9;
            case CHANNEL_10:
                return ExperimentSampleTypeItem.CHANNEL_10;
        }

        throw new IllegalStateException("Undefined sample type: " + type);
    }

    /*Returns map where key is Sample Condition ID, value is condition object*/
    public Map<Long, SampleCondition> getSampleToConditionMap(AbstractExperiment experiment) {
//        final Set<SampleCondition> conditions = newHashSet();
        final Map<Long, SampleCondition> sampleFileToConditionMap = newHashMap();

        for (RawFile rawFile : experiment.getRawFiles().getData()) {
            for (PrepToExperimentSample prepToExperimentSample : rawFile.getPreparedSample().getSamples()) {
                final ExperimentSample sample = prepToExperimentSample.getExperimentSample();
                Set<SampleCondition> sampleConditions = sample.getSampleConditions();
                final SampleCondition conditionToAdd;
                if (sampleConditions.isEmpty()) {
                    conditionToAdd = SampleCondition.createUndefinedCondition(experiment, ImmutableSet.of(sample));
                } else {
                    conditionToAdd = sampleConditions.iterator().next();
                }
                sampleFileToConditionMap.put(sample.getId(), conditionToAdd);

            }
        }
        return sampleFileToConditionMap;
    }


    private static List<MSFunctionItem> getMsFunctionItems(final long lab, AbstractFileMetaData fileMetaData) {
        return newLinkedList(from(fileMetaData.getUsersFunctions()).firstMatch(new Predicate<UserLabFileTranslationData>() {
            @Override
            public boolean apply(UserLabFileTranslationData input) {
                return input.getLab() != null && input.getLab().getId().equals(lab);
            }
        }).transform(Transformers.MS_FUNCTIONS_FROM_USER_TRANSLATION_DATA).or(ImmutableSet.<MSFunctionItem>of()));
    }
}
