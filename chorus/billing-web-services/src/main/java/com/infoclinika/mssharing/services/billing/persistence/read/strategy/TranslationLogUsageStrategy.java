package com.infoclinika.mssharing.services.billing.persistence.read.strategy;

import com.infoclinika.mssharing.model.internal.entity.payment.ChargeableItem;
import com.infoclinika.mssharing.services.billing.persistence.repository.TranslationUsageRepository;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * @author Herman Zamula
 */
@Component
public class TranslationLogUsageStrategy extends AbstractCommonFeatureUsage {

    @Inject
    public TranslationLogUsageStrategy(TranslationUsageRepository usageRepository) {
        super(ChargeableItem.Feature.TRANSLATION, usageRepository);
    }

}
