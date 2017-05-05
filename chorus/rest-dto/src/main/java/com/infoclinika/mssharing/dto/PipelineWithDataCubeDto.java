package com.infoclinika.mssharing.dto;

import com.infoclinika.analysis.rest.dto.PipelineNodeDto;

public class PipelineWithDataCubeDto {
    private PipelineNodeDto pipelineNodeDto;
    private String dataCubeReference;

    public PipelineWithDataCubeDto(PipelineNodeDto pipelineNodeDto, String dataCubeReference) {
        this.pipelineNodeDto = pipelineNodeDto;
        this.dataCubeReference = dataCubeReference;
    }

    public PipelineNodeDto getPipelineNodeDto() {
        return pipelineNodeDto;
    }

    public void setPipelineNodeDto(PipelineNodeDto pipelineNodeDto) {
        this.pipelineNodeDto = pipelineNodeDto;
    }

    public String getDataCubeReference() {
        return dataCubeReference;
    }

    public void setDataCubeReference(String dataCubeReference) {
        this.dataCubeReference = dataCubeReference;
    }

    @Override
    public String toString() {
        return "PipelineWithDataCubeDto{" +
                "pipelineNodeDto=" + pipelineNodeDto +
                ", dataCubeReference='" + dataCubeReference + '\'' +
                '}';
    }
}
