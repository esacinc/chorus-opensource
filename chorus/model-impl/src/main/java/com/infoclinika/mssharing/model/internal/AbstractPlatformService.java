package com.infoclinika.mssharing.model.internal;

import com.infoclinika.msexplorer.messaging.api.ExperimentWorkflowType;
import com.infoclinika.msexplorer.messaging.api.InstrumentVendorType;
import com.infoclinika.mssharing.model.helper.StoredObjectPaths;


import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Herman Zamula
 */


public abstract class AbstractPlatformService {

    protected static final long RECEIVE_TIMEOUT = 600 * 1000 * 60 /*600 minutes*/;
    @Inject
    protected StoredObjectPaths paths;

    public final static Map<String, InstrumentVendorType> VENDOR_TYPE_MAP;
    protected final static Map<String, ExperimentWorkflowType> WORKFLOW_TYPE_MAP;

    static {
        Map<String, InstrumentVendorType> vendorTypeMap = new HashMap<String, InstrumentVendorType>();
        vendorTypeMap.put("Thermo Scientific", InstrumentVendorType.THERMO);
        vendorTypeMap.put("Waters", InstrumentVendorType.WATERS);
        vendorTypeMap.put("Agilent", InstrumentVendorType.AGILENT);
        vendorTypeMap.put("Bruker", InstrumentVendorType.BRUKER);
        vendorTypeMap.put("Sciex", InstrumentVendorType.SCIEX);
        VENDOR_TYPE_MAP = Collections.unmodifiableMap(vendorTypeMap);

        Map<String, ExperimentWorkflowType> workflowTypeMap = new HashMap<String, ExperimentWorkflowType>();

        //todo [tymchenko] review once we take the workflow type into account during translation
        workflowTypeMap.put("Unspecified", ExperimentWorkflowType.LABELS_FREE);
        workflowTypeMap.put("Label Free Quantitative", ExperimentWorkflowType.LABELS_FREE);
        workflowTypeMap.put("Label Free Differential", ExperimentWorkflowType.LABELS_FREE);
        workflowTypeMap.put("MSe", ExperimentWorkflowType.MS_E);
        workflowTypeMap.put("MRM", ExperimentWorkflowType.LABELS_FREE);
        workflowTypeMap.put("Shotgun", ExperimentWorkflowType.SHOTGUN);
        workflowTypeMap.put("Spectrum Counting", ExperimentWorkflowType.LABELS_FREE);
        workflowTypeMap.put("SILAC", ExperimentWorkflowType.SILAC);
        workflowTypeMap.put("iTRAQ", ExperimentWorkflowType.LABELS_FREE);
        workflowTypeMap.put("ICAT", ExperimentWorkflowType.LABELS_FREE);

        WORKFLOW_TYPE_MAP = Collections.unmodifiableMap(workflowTypeMap);
    }

}
