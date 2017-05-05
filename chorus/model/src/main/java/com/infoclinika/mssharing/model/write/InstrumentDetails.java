package com.infoclinika.mssharing.model.write;

import com.infoclinika.mssharing.model.helper.LockMzItem;
import com.infoclinika.mssharing.platform.model.write.InstrumentManagementTemplate;

import java.util.List;

/**
* @author Herman Zamula
*/
public class InstrumentDetails extends InstrumentManagementTemplate.InstrumentDetailsTemplate {
    public final String hplc;
    public final List<LockMzItem> lockMasses;
    public final boolean autoTranslate;

    public InstrumentDetails(String name, String serialNumber, String hplc, String peripherals, List<LockMzItem> lockMasses, boolean autoTranslate) {
        super(name, serialNumber, peripherals);
        this.hplc = hplc;
        this.lockMasses = lockMasses;
        this.autoTranslate = autoTranslate;
    }
}
