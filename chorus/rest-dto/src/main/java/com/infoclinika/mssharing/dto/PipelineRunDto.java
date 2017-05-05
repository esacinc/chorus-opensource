package com.infoclinika.mssharing.dto;

import com.infoclinika.analysis.rest.dto.PipelineNodeDto;

import java.util.Date;

public class PipelineRunDto {
    private PipelineRunDto parent;
    private String pipelineName;
    private Date creationDate;
    private PipelineNodeDto pipeline;
    private String inputDCReference;
    private String outputDCReference;
    private String dataView;
    private String composedType;

    public PipelineRunDto(
            PipelineRunDto parent,
            String pipelineName,
            Date creationDate,
            PipelineNodeDto pipeline,
            String inputDCReference,
            String outputDCReference,
            String dataView,
            String composedType
    ) {
        this.parent = parent;
        this.pipelineName = pipelineName;
        this.creationDate = creationDate;
        this.pipeline = pipeline;
        this.inputDCReference = inputDCReference;
        this.outputDCReference = outputDCReference;
        this.dataView = dataView;
        this.composedType = composedType;
    }

    public PipelineRunDto() {}

    public PipelineRunDto getParent() {
        return parent;
    }

    public void setParent(PipelineRunDto parent) {
        this.parent = parent;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public void setPipelineName(String pipelineName) {
        this.pipelineName = pipelineName;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public PipelineNodeDto getPipeline() {
        return pipeline;
    }

    public void setPipeline(PipelineNodeDto pipeline) {
        this.pipeline = pipeline;
    }

    public String getInputDCReference() {
        return inputDCReference;
    }

    public void setInputDCReference(String inputDCReference) {
        this.inputDCReference = inputDCReference;
    }

    public String getOutputDCReference() {
        return outputDCReference;
    }

    public void setOutputDCReference(String outputDCReference) {
        this.outputDCReference = outputDCReference;
    }

    public String getDataView() {
        return dataView;
    }

    public void setDataView(String dataView) {
        this.dataView = dataView;
    }

    public String getComposedType() {
        return composedType;
    }

    public void setComposedType(String composedType) {
        this.composedType = composedType;
    }
}
