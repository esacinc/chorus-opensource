package com.infoclinika.mssharing.dto.request;

import com.infoclinika.analysis.rest.dto.PipelineNodeDto;

public class SavePipelineRunRequest {
    private PipelineNodeDto pipeline;
    private String pipelineName;
    private String inputDCReference;
    private String outputDCReference;

    public SavePipelineRunRequest(
            PipelineNodeDto pipeline,
            String pipelineName,
            String inputDCReference,
            String outputDCReference
    ) {
        this.pipeline = pipeline;
        this.pipelineName = pipelineName;
        this.inputDCReference = inputDCReference;
        this.outputDCReference = outputDCReference;
    }

    public SavePipelineRunRequest() {
    }

    public PipelineNodeDto getPipeline() {
        return pipeline;
    }

    public void setPipeline(PipelineNodeDto pipeline) {
        this.pipeline = pipeline;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public void setPipelineName(String pipelineName) {
        this.pipelineName = pipelineName;
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
}
