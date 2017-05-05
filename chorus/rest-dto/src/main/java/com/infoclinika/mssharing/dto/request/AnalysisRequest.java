package com.infoclinika.mssharing.dto.request;


import com.infoclinika.analysis.rest.dto.PipelineNodeDto;

public class AnalysisRequest {
    public String composedType;
    public String dataViewLayer;
    public PipelineNodeDto rootNode;

    public AnalysisRequest(String composedType, String dataViewLayer, PipelineNodeDto rootNode) {
        this.composedType = composedType;
        this.dataViewLayer = dataViewLayer;
        this.rootNode = rootNode;
    }

    public AnalysisRequest() {}

    public String getComposedType() {
        return composedType;
    }

    public void setComposedType(String composedType) {
        this.composedType = composedType;
    }

    public String getDataViewLayer() {
        return dataViewLayer;
    }

    public void setDataViewLayer(String dataViewLayer) {
        this.dataViewLayer = dataViewLayer;
    }

    public PipelineNodeDto getRootNode() {
        return rootNode;
    }

    public void setRootNode(PipelineNodeDto rootNode) {
        this.rootNode = rootNode;
    }
}
