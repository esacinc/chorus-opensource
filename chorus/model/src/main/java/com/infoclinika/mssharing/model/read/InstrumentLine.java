package com.infoclinika.mssharing.model.read;

import com.infoclinika.mssharing.platform.model.read.InstrumentReaderTemplate;

/**
* @author Herman Zamula
*/
public class InstrumentLine extends InstrumentReaderTemplate.InstrumentLineTemplate {
    public final boolean autotranslate;

    public InstrumentLine(InstrumentReaderTemplate.InstrumentLineTemplate other, boolean autotranslate) {
        super(other);
        this.autotranslate = autotranslate;
    }
}
