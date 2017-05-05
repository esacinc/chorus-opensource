package com.infoclinika.mssharing.skyline.web.controller.response;

/**
 * @author Oleksii Tymchenko
 */
public class RestrictionDTO {
    public long technologyType;
    public long vendor;
    public long instrumentModel;
    public Long instrument;

    public RestrictionDTO() {
    }

    public RestrictionDTO(long technologyType, long vendor, long instrumentModel, Long instrument) {
        this.technologyType = technologyType;
        this.vendor = vendor;
        this.instrumentModel = instrumentModel;
        this.instrument = instrument;
    }

    @Override
    public String toString() {
        return "RestrictionDTO{" +
                "technologyType=" + technologyType +
                ", vendor=" + vendor +
                ", instrumentModel=" + instrumentModel +
                ", instrument=" + instrument +
                '}';
    }
}
