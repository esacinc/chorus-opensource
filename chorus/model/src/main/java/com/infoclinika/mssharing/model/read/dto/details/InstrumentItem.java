package com.infoclinika.mssharing.model.read.dto.details;

import com.google.common.collect.ImmutableList;
import com.infoclinika.mssharing.model.helper.LockMzItem;
import com.infoclinika.mssharing.platform.model.read.DetailsReaderTemplate.InstrumentItemTemplate;

/**
* @author Herman Zamula
*/
public class InstrumentItem extends InstrumentItemTemplate {
    public final ImmutableList<LockMzItem> lockMasses;
    public final boolean autoTranslate;
    public final String hplc;

    public InstrumentItem(InstrumentItemTemplate other, ImmutableList<LockMzItem> lockMasses, boolean autoTranslate, String hplc) {
        super(other);
        this.lockMasses = lockMasses;
        this.autoTranslate = autoTranslate;
        this.hplc = hplc;
    }
}
