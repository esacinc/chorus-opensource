package com.infoclinika.mssharing.model.internal.read;

import com.google.common.base.Function;
import com.infoclinika.mssharing.model.internal.entity.MSFunctionItem;
import com.infoclinika.mssharing.model.internal.entity.MZGridParams;
import com.infoclinika.mssharing.model.internal.repository.MSFunctionItemRepository;
import com.infoclinika.mssharing.model.read.MSFunctionItemReader;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.transaction.Transactional;

import static com.google.common.collect.FluentIterable.from;

/**
 * @author vladislav.kovchug
 */
@Service
@Transactional
public class MSFunctionItemReaderImpl implements MSFunctionItemReader {

    @Inject
    private MSFunctionItemRepository msFunctionItemRepository;

    @Override
    public Iterable<MSFunctionItemInfo> readAll() {
        return from(msFunctionItemRepository.findAll())
                .transform(new Function<MSFunctionItem, MSFunctionItemInfo>() {
                    @Nullable
                    @Override
                    public MSFunctionItemInfo apply(MSFunctionItem input) {
                        final MZGridParams mzGridParams = input.getMzGridParams();
                        return new MSFunctionItemInfo(input.getId(), input.getFunctionName(),
                                mzGridParams == null ? null : mzGridParams.getId());
                    }
                }).toList();
    }
}
