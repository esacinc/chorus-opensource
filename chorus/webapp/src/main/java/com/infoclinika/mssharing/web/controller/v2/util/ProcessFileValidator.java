package com.infoclinika.mssharing.web.controller.v2.util;

import java.util.Collection;
import java.util.Map;

public interface ProcessFileValidator {

    Map<String, Collection<String>> validateAssociateFiles(Map<String, Collection<String>> map, long experimentId, long user);

    Map<String, Collection<String>> validateAllProcessedFilesByExperiment(Map<String, Collection<String>> map, long experiment);
}
