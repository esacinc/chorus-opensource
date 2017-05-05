package com.infoclinika.mssharing.dto.request;

public class PipelineRunRequest {
    private AnalysisRequest analysisRequest;
    private String inputDataCubeReference;

    public PipelineRunRequest(AnalysisRequest analysisRequest, String inputDataCubeReference) {
        this.analysisRequest = analysisRequest;
        this.inputDataCubeReference = inputDataCubeReference;
    }

    public PipelineRunRequest() {}

    public AnalysisRequest getAnalysisRequest() {
        return analysisRequest;
    }

    public void setAnalysisRequest(AnalysisRequest analysisRequest) {
        this.analysisRequest = analysisRequest;
    }

    public String getInputDataCubeReference() {
        return inputDataCubeReference;
    }

    public void setInputDataCubeReference(String inputDataCubeReference) {
        this.inputDataCubeReference = inputDataCubeReference;
    }
}
