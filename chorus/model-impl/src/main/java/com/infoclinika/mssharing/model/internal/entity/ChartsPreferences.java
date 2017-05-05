package com.infoclinika.mssharing.model.internal.entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * @author vladislav.kovchug
 */
@Embeddable
public class ChartsPreferences {

    @Column
    private Float rtLeft;

    @Column
    private Float rtRight;

    @Column
    private Float mzLeft;

    @Column
    private Float mzRight;

    public ChartsPreferences(Float rtLeft, Float rtRight, Float mzLeft, Float mzRight) {
        this.rtLeft = rtLeft;
        this.rtRight = rtRight;
        this.mzLeft = mzLeft;
        this.mzRight = mzRight;
    }

    public ChartsPreferences() {
    }

    public Float getRtLeft() {
        return rtLeft;
    }

    public void setRtLeft(Float rtLeft) {
        this.rtLeft = rtLeft;
    }

    public Float getRtRight() {
        return rtRight;
    }

    public void setRtRight(Float rtRight) {
        this.rtRight = rtRight;
    }

    public Float getMzLeft() {
        return mzLeft;
    }

    public void setMzLeft(Float mzLeft) {
        this.mzLeft = mzLeft;
    }

    public Float getMzRight() {
        return mzRight;
    }

    public void setMzRight(Float mzRight) {
        this.mzRight = mzRight;
    }
}
