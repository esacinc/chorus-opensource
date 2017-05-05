angular.module("protein-search-back", ["ngResource"])
    .factory("ProteinDatabase", function ($resource) {
        return $resource("../proteindbs/:path/", {}, {
            allAvailable: {method: "GET", isArray: true, params: {path: "allAvailable"}}
        });
    })
    .factory("ProcessingRunCharts", function ($resource) {
        return $resource("../run/results/charts/:run/:path/", {run: "@run"}, {
            scanIndexesForPeptideAndFile: {
                method: "POST",
                isArray: true,
                params: {path: "scanIndexesForPeptideAndFile"}
            },
            msdaplChartData: {method: "GET", params: {path: "msDaplChartData"}},
            tmtCharData: {method: "GET", params: {path: "tmtChartData"}},
            chroChartData: {method: "GET", params: {path: "chroChartData"}},
            distinctChroChartData: {method: "GET", params: {path: "distinctChroChartData"}},
            trendPlotChart: {method: "POST", params: {path: "trendPlotChartInSvg"}},
            trendPlotChartNearestRows: {method: "POST", params: {path: "nearestRowsForTrendPlotChart"}}
        });
    })
    .factory("ProcessingRunResultsCommon", function ($resource) {
        return $resource("../run/results/common/:run/:path/:action/:type", {run: "@run"}, {
            summaryStatistics: {method: "GET", params: {path: "summaryStatistics"}},
            aggregatedProteinDetails: {method: "GET", params: {path: "aggregatedProtein", action: "details"}},
            aggregatedProteinDetailsFull: {
                method: "GET",
                params: {path: "aggregatedProtein", action: "details", type: "full"}
            },
            peptideDetails: {method: "GET", params: {path: "peptide", action: "details"}},
            pipelineExecutionExtraResults: {method: "GET", isArray: true, params: {path: "extras", action: "list"}},
            getExtraResultFileLink: {method: "GET", params: {path: "extras", action: "getLink"}},
            canAccessSearchResults: {method: "GET", params: {path: "canAccessSearchResults"}}
        });
    })
    .factory("ProcessingRunAnalysis", function ($resource) {
        return $resource("../run/results/analysis/:run/:path/:action", {run: "@run"}, {
            allPlugins: {method: "GET", isArray: true, params: {path: "plugins"}},
            pluginDetails: {method: "GET", params: {path: "plugins", action: "details"}},
            filterPlugins: {method: "POST", params: {path: "plugins", action: "filter"}},
            validateAnalysis: {method: "POST", params: {path: "validate"}},
            //templates
            analysisTemplates: {method: "GET", isArray: true, params: {path: "templates"}},
            persistedDatacubes: {method: "GET", isArray: true, params: {path: "persistedDatacubes"}},
            analysisTemplateDetails: {method: "GET", params: {path: "templates", action: "details"}},
            createCurrentAsAnalysisTemplate: {method: "POST", params: {path: "createTemplate"}},
            updateCurrentAnalysisTemplate: {method: "POST", params: {path: "updateTemplate"}},
            deleteAnalysisTemplate: {method: "DELETE", params: {path: "deleteTemplate"}},
            //analysis
            currentAnalysis: {method: "GET", params: {path: "current"}},
            analyses: {method: "GET", isArray: true},
            analysisDetails: {method: "GET", params: {path: "details"}},
            deleteAnalysis: {method: "DELETE", params: {path: "delete"}},
            createCurrentAsAnalysis: {method: "POST", params: {path: "create"}},
            updateCurrentAnalysis: {method: "POST", params: {path: "update"}},

        });
    })
    .factory("AvailableDataCubes", function ($resource) {
        return $resource("../experiment/searches/predictDataCubesForWorkflow", {}, {
            "get": {method: "POST", params: {}, isArray: true}
        });
    })
    .factory("PostProcessingPipelines", function ($resource) {
        return $resource("../experiment/searches/postProcessingPipelines", {}, {
            "get": {method: "GET", params: {}, isArray: true}
        });
    })
    .factory("ExperimentSearch", function ($resource) {
        return $resource("../experiment/searches/:filter/", {}, {
            "get": {method: "GET", params: {filter: "all"}, isArray: true},
            "reSearchSelected": {method: "POST", params: {filter: "selected"}},
            "stopSearch": {method: "POST", params: {filter: "stop"}},
            "availableWorkflowTypes": {method: "GET", params: {filter: "availableWorkflowTypes"}}
        });
    })
    .factory("PluginAttachmentUpload", function ($resource) {
        return $resource("../sequestSearch/algorithms/upload/:path/:id", {id: "@id"}, {
            "items": {method: "GET", isArray: true, params: {path: "items"}},
            "postMetadata": {method: "POST", params: {path: "items"}},
            "updateContent": {method: "POST", params: {path: "update"}},
            "updateDetails": {method: "PUT"},
            "remove": {method: "DELETE"},
            "getDestinationPath": {method: "GET", params: {path: "destination"}},
            "getDestinationPathWithBucket": {method: "GET", params: {path: "destinationWithBucket"}},
            "maxSizeInBytes": {method: "GET", params: {path: "maxSizeInBytes"}}
        });
    })
    .factory("ProteinSearchAttachmentUpload", function ($resource) {
        return $resource("../protein-search/attachment/:path/:id", {id: "@id"}, {
            "items": {method: "GET", isArray: true, params: {path: "items"}},
            "postMetadata": {method: "POST", params: {path: "items"}},
            "updateContent": {method: "POST", params: {path: "update"}},
            "updateDetails": {method: "PUT"},
            "remove": {method: "DELETE"},
            "getDestinationPath": {method: "GET", params: {path: "destination"}},
            "getDestinationPathWithBucket": {method: "GET", params: {path: "destinationWithBucket"}},
            "maxSizeInBytes": {method: "GET", params: {path: "maxSizeInBytes"}},
            "completeUpload": {method: "PUT", params: {path: "completeUpload"}}
        });
    })
    .factory("WorkflowStepDescription", function ($resource) {
        return $resource("../experiment/searches/workflowStep/:workflowId/", {}, {});
    })
    .factory("AllProteinSearches", function ($resource) {
        return $resource("../experiment/searches/:paged/all", {paged: "@paged"}, {
            "get": {method: "POST"}
        });
    })
    .factory("ProteinDB", function ($resource) {
        return $resource("../proteindbs/:path/:id", {id: "@id"}, {
            "items": {method: "GET", isArray: true, params: {path: "items"}},
            "itemsAccessibleByUser": {method: "GET", isArray: true, params: {path: "itemsAccessibleByUser"}},
            "myItems": {method: "GET", isArray: true, params: {path: "my"}},
            "publicItems": {method: "GET", isArray: true, params: {path: "public"}},
            "postMetadata": {method: "POST", params: {path: "items"}},
            "updateContent": {method: "POST", params: {path: "update"}},
            "updateDetails": {method: "PUT"},
            "remove": {method: "DELETE"},
            "getDestinationPath": {method: "GET", params: {path: "destination"}},
            "maxSizeInBytes": {method: "GET", params: {path: "maxSizeInBytes"}},
            "share": {method: "PUT", params: {path: "share"}},
            "duplicate" : {method: "POST", params: {path: "duplicate"}}
        });
    })
    .factory("ProteinIDSearchParams", function ($resource) {
        return $resource("../experiment/searches/:experimentId/:filter", {}, {
            "defaultCommonParams": {method: "GET", params: {filter: "defaultCommonParams"}},
            "getWorkflowStepTypes": {method: "GET", params: {filter: "getWorkflowStepTypes"}, isArray: true}
        });
    })
    .factory("ExperimentSearches", function ($resource) {
        return $resource("../experiment/searches/:path", {}, {
            validate: {method: "POST", params: {path: "validate"}},
            update: {method: "PUT"},
            cleanTempResults: {method: "DELETE", params: {path: "clean"}}
        });
    })
    .factory("ExperimentSearchDetails", function ($resource) {
        return $resource("../experiment/searches/:run/details/:filter", {}, {
            getWorkflowSteps: {method: "GET", params: {filter: "workflowSteps"}}
        })
    })
    .factory("ExperimentSearchShortDetails", function ($resource) {
        return $resource("../experiment/searches/:experiment/shortDetails")
    });

