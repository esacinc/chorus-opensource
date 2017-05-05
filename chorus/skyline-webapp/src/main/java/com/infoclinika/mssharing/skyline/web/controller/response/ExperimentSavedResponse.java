package com.infoclinika.mssharing.skyline.web.controller.response;

/**
 * //todo: eliminate duplication with ExperimentIdResponse in webapp
 *
 * @author Oleksii Tymchenko
 */
public class ExperimentSavedResponse {
    public final long experimentId;

    public ExperimentSavedResponse(long experimentId) {
        this.experimentId = experimentId;
    }

}
