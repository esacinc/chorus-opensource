angular.module("adminTranslationManager", ["ngResource"]).
    factory("ExperimentTranslationManager", function ($resource) {
        return $resource("../experiments/:path/:filter", {}, {
            "all": {method: "GET", params: {path: "translation", filter: "all"}, isArray: true},
            "paged": {method: "GET", params: {path: "translation", filter: ""}},
            /*
             * @deprecated
             */
            "retranslateSelected": {method: "POST", params: {path: "translation", filter: "selected"}},
            "reTranslateSelected": {method: "POST", params: {path: "per-experiment-translation", filter: "selected"}}
        });
    })
    .factory("FileTranslationManager", function($resource) {
        return $resource("../files/per-file-translation/:path", {}, {
            "reTranslateAll": {method: "POST", params: {path: "all"}},
            "reTranslateSelected": {method: "POST", params: {path: "selected"}},
            "statuses": {method: "GET", params: {path: "statuses"}}
        })
    });
