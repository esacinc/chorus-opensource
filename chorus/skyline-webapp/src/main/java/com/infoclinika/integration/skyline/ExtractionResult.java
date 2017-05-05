package com.infoclinika.integration.skyline;

import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.GroupPoints;

/**
* @author Oleksii Tymchenko
*/
class ExtractionResult implements Comparable<ExtractionResult> {
    private final int order;
    private final GroupPoints groupPoints;
    private final SingleSpectrumExtractionResult spectrumExtractionResult;

    ExtractionResult(int order, GroupPoints groupPoints) {
        this.order = order;
        this.groupPoints = groupPoints;
        this.spectrumExtractionResult = null;
    }

    ExtractionResult(int order, SingleSpectrumExtractionResult result) {
        this.order = order;
        this.groupPoints = null;
        this.spectrumExtractionResult = result;
    }

    public int getOrder() {
        return order;
    }

    public GroupPoints getGroupPoints() {
        return groupPoints;
    }

    public SingleSpectrumExtractionResult getSpectrumExtractionResult() {
        return spectrumExtractionResult;
    }

    @Override
    public int compareTo(ExtractionResult o) {
        return order - o.getOrder();
    }
}
