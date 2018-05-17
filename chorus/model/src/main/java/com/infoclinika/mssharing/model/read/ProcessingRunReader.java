package com.infoclinika.mssharing.model.read;

import org.springframework.security.access.method.P;

public interface ProcessingRunReader {

    boolean findProcessingRunByExperiment(String name, long experiment);

}
