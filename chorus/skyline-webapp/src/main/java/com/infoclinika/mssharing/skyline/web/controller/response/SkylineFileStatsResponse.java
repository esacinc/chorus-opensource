package com.infoclinika.mssharing.skyline.web.controller.response;

import java.util.List;

/**
 * @author Oleksii Tymchenko
 */
public class SkylineFileStatsResponse {
    public final List<Double> driftTimes;

    public SkylineFileStatsResponse(List<Double> driftTimes) {
        this.driftTimes = driftTimes;
    }
}
