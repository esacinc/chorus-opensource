package com.infoclinika.mssharing.model.internal.read;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.infoclinika.mssharing.model.helper.ExperimentPreparedSampleItem;
import com.infoclinika.mssharing.model.internal.entity.ExperimentPreparedSample;
import com.infoclinika.mssharing.model.internal.entity.MSFunctionItem;
import com.infoclinika.mssharing.model.internal.entity.RawFile;
import com.infoclinika.mssharing.model.internal.entity.restorable.AbstractFileMetaData;
import com.infoclinika.mssharing.model.internal.entity.restorable.ActiveFileMetaData;
import com.infoclinika.mssharing.model.read.dto.details.FileItem;
import com.infoclinika.mssharing.platform.entity.ExperimentFileTemplate;
import com.infoclinika.mssharing.platform.model.helper.read.details.DetailsTransformers;
import com.infoclinika.mssharing.platform.model.read.DetailsReaderTemplate;
import org.springframework.stereotype.Component;

import java.util.Iterator;

import static com.google.common.collect.FluentIterable.from;
import static com.infoclinika.mssharing.model.internal.read.Transformers.MS_FUNCTIONS_FROM_USER_TRANSLATION_DATA;
import static com.infoclinika.mssharing.model.internal.read.Transformers.transformStorageStatus;

/**
 * @author Herman Zamula
 */
@Component
public class DetailsTransformersImpl extends DetailsTransformers {

    @Override
    public <F extends ExperimentFileTemplate> Function<F, DetailsReaderTemplate.FileItemTemplate> experimentFileTransformer() {

        return new Function<F, DetailsReaderTemplate.FileItemTemplate>() {
            @Override
            public DetailsReaderTemplate.FileItemTemplate apply(F input) {

                final DetailsReaderTemplate.FileItemTemplate byDefault = DetailsTransformersImpl.super.experimentFileTransformer().apply(input);

                final ActiveFileMetaData fileMetaData = (ActiveFileMetaData) input.getFileMetaData();
                final Iterator<MSFunctionItem> functionItemIterator = getMsFunctionItems(fileMetaData).iterator();
                final RawFile rawFile = (RawFile) input;
                final ExperimentPreparedSample preparedSample = rawFile.getPreparedSample();
                final ExperimentPreparedSampleItem preparedSampleItem = new ExperimentPreparedSampleItem(preparedSample.getName(),
                        from(preparedSample.getSamples()).transform(Transformers.AS_SAMPLE_ITEM).toSet());
                return new FileItem(byDefault, fileMetaData.getArchiveId(),
                        transformStorageStatus(fileMetaData.getStorageData().getStorageStatus(), fileMetaData.getStorageData().isArchivedDownloadOnly()),
                        functionItemIterator.hasNext() ? functionItemIterator.next().getTranslatedPath() : null, rawFile.getFractionNumber(), preparedSampleItem);
            }
        };
    }

    private FluentIterable<MSFunctionItem> getMsFunctionItems(AbstractFileMetaData file) {

        return from(file.getUsersFunctions())
                .transformAndConcat(MS_FUNCTIONS_FROM_USER_TRANSLATION_DATA);
    }


}
