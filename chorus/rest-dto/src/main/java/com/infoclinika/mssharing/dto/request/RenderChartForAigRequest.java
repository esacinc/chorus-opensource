package com.infoclinika.mssharing.dto.request;

import java.util.List;
import java.util.Optional;

/**
 * @author Oleksii Tymchenko
 */
public class RenderChartForAigRequest {
    public long proteinSearchId;

    public Long selectedRowId;
    public List<Long> selectedColumnIds;

    public int width;
    public int height;

    public String viewLayer;
    public String coverageType;

    public String extractionType;
    public String spectrumType;

    public Float startRt;
    public Float endRt;
}
