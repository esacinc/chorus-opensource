package com.infoclinika.mssharing.skyline.web.controller.request;

import com.infoclinika.common.model.experiment.MSRectItem;
import com.infoclinika.computations.api.ExtractionType;
import com.infoclinika.msdata.image.SpectrumType;

import java.util.Set;

/**
 * @author Oleksii Tymchenko
 */
public class RenderChartAcrossFilesWithRtShiftsRequest {
    public String[] files;
    public String[] colors;
    public Boolean[] selected;
    public Long[] conditionIds;
    public int width;
    public int height;
    public SpectrumType chartType;
    public float startRt;
    public float endRt;
    public double startMz;
    public double endMz;
    public Float[] rtShifts;
    public int monoMz;
    public byte charge;
    public Set<MSRectItem> subRanges;
    public ExtractionType extractionType;
}
