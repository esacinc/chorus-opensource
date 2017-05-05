package com.infoclinika.mssharing.skyline.web.controller.request;

/**
 * @author Oleksii Tymchenko
 */
public class RenderChartByFilesRequest {
    public long[] files;
    public String[] colors;
    public int width;
    public int height;
    public String chartType;
    public float startRt;
    public float endRt;
    public double startMz;
    public double endMz;
}
