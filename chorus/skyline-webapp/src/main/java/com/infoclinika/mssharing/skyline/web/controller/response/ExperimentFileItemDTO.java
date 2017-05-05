package com.infoclinika.mssharing.skyline.web.controller.response;

import java.util.Date;
import java.util.List;

/**
 * @author timofey 24.12.15.
 */
public class ExperimentFileItemDTO {

    public Long id;
    public Date date;
    public String labels;
    public Integer fractionNumber;
    public PreparedSample preparedSample;

    public static class PreparedSample {
        public String name;
        public List<Sample> samples;
    }

    public static class Sample {
        public String name;
        public SampleType type;
        public List<String> factorValues;
    }

    public enum SampleType {
        LIGHT,
        MEDIUM,
        HEAVY
    }

}
