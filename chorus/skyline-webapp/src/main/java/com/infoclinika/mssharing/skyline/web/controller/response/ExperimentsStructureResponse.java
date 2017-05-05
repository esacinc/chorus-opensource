package com.infoclinika.mssharing.skyline.web.controller.response;

import com.infoclinika.mssharing.model.read.DashboardReader;

import java.util.Set;

/**
 * @author Oleksii Tymchenko
 */
public class ExperimentsStructureResponse {
    public final Set<DashboardReader.ExperimentStructure> experiments;

    public ExperimentsStructureResponse(Set<DashboardReader.ExperimentStructure> experiments) {
        this.experiments = experiments;
    }
}
