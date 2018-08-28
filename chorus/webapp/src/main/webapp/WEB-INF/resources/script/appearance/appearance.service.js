(function () {

    "use strict";

    angular.module("appearance").factory("Appearance", AppearanceService);

    AppearanceService.$inject = ["$resource"];

    function AppearanceService($resource) {
        return $resource(subStringLastIndexOfFor(window.location.pathname, "/", 2) + "/appearance");
    }

    function subStringLastIndexOfFor(string, searchString, times) {
        var result = string;
        for (var i = 0; i < times; i++) {
            result = result.substring(0, result.lastIndexOf(searchString));
        }
        return result;
    }

})();
