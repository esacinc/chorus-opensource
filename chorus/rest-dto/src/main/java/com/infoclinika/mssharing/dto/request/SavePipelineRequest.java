package com.infoclinika.mssharing.dto.request;

import com.infoclinika.analysis.rest.dto.PipelineNodeDto;

public class SavePipelineRequest {
    private String composedType;
    private String dataViewLayer;
    private PipelineNodeDto rootNode;
    private String name;
    private String dataToDisplay;

    public SavePipelineRequest(String composedType, String dataViewLayer, PipelineNodeDto rootNode, String name) {
        this.composedType = composedType;
        this.dataViewLayer = dataViewLayer;
        this.rootNode = rootNode;
        this.name = name;
        //TODO:Andrey.Cherepovskiy: send real dataToDisplay
        this.dataToDisplay = "intensitiesSheet";
    }

    public SavePipelineRequest() {
    }

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDataToDisplay() {
        return dataToDisplay;
    }

    public void setDataToDisplay(String dataToDisplay) {
        this.dataToDisplay = dataToDisplay;
    }
}
