package com.infoclinika.integration.skyline;

/**
 * @author Oleksii Tymchenko
 */
public class SingleSpectrumExtractionResult {
    public final int index;
    public final double rt;
    public final double[] mzs;
    public final float[] intensitites;


    public SingleSpectrumExtractionResult(int index, double rt, double[] mzs, float[] intensitites) {
        this.index = index;
        this.rt = rt;
        this.mzs = mzs;
        this.intensitites = intensitites;
    }
}
