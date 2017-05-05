angular.module("adminTranslationContollers", ["adminTranslationManager", "validators", "error-catcher"]).
    controller("translationStatuses", function ($scope, $location, $routeParams, ExperimentTranslationManager,
                                                DashboardButtonFactory, DashboardButton, contentRequestParameters) {
        if ($scope.pathError) return;
        CommonLogger.setTags(["ADMIN-TRANSLATION", "TRANSLATION-STATUSES-CONTROLLER"]);
        $scope.page.title = "Per Experiment File Translation Status";
        $scope.allItemsSelected = false;
        $scope.page.filterScope = $scope;
        $scope.page.showPageableFilter = true;
        $scope.total = 0;
        $scope.allItemsSelected = false;

        var isTableEmpty = false;
        var pageRequest = contentRequestParameters.getParameters("translation");

        var loadData = function () {
            ExperimentTranslationManager.paged(pageRequest, function (data) {
                isTableEmpty = data.items.length == 0;
                $scope.total = data.itemsCount;
                $scope.experimentTranslations = $.map(data.items, function(elem) {

                    var getStatus = function(experiment) {
                        if(experiment.translationSubmitted) {
                            if(experiment.traslationError) {
                                return "Translation Error";
                            } else if(experiment.chartsUrl) {
                                return "Success";
                            } else {
                                return "In Progress...";
                            }
                        } else {
                            return "Not Translated";
                        }
                    };

                    return {id: elem.experimentId,
                        experimentName: elem.experimentName,
                        labName: elem.labName,
                        owner: elem.owner,
                        status: getStatus(elem)};
                });
                CommonLogger.log("Experiment translations initialized.");
                startWatch($scope, switchRetranslateButton);
            });

        };

        var retranslateSelected = function() {
            var experimentIds = {"experiments": []};
            angular.forEach($scope.experimentTranslations, function(item) {
                if(item.selected) this.experiments.push(item.id);
            }, experimentIds);

            ExperimentTranslationManager.reTranslateSelected(experimentIds, loadData);
        };


        var switchRetranslateButton = function (enabled) {
            var button = new DashboardButton(0, "retranslate", "Retranslate Selected Experiments", "retranslate");
            button.display = enabled;
            button.onClickHandler = function () {
                $scope.retranslateSelectedPopup = new RetranslateSelectedPopup("#retranslate-all-confirmation", retranslateSelected);
                $scope.retranslateSelectedPopup.showPopup();
            };

            DashboardButtonFactory.count(enabled);
            DashboardButtonFactory.origin("translation");
            DashboardButtonFactory.put(button);
        };

        $scope.isTableEmpty = function () {
            return isTableEmpty;
        };

        $scope.getEmptyTableMessage = function () {
            return "There are no experiments";
        };

        loadData();

        $scope.selectAll = function (selectAll, items) {
            angular.forEach(items, function (item) {
                item.selected = selectAll;
            });
        };

        $scope.changeSelection = function (item) {
            item.selected = !item.selected;
        };

        var RetranslateSelectedPopup = function(selector,retranslateCallback) {
            this.popupSelector = selector;
            this.retranslateCallback = retranslateCallback;
        };

        RetranslateSelectedPopup.prototype.showPopup = function() {
            $(this.popupSelector).modal("show");
        };
        RetranslateSelectedPopup.prototype.hidePopup = function () {
            $(this.popupSelector).modal("hide");
        };

        RetranslateSelectedPopup.prototype.retranslate = function(metadataOnly) {
            this.retranslateCallback(metadataOnly);
            this.hidePopup();
        };
    })
    .controller("perFileTranslationStatuses", function($scope, $routeParams, FileTranslationManager,
                                                       contentRequestParameters, DashboardButtonFactory, DashboardButton, DashboardGlobalButtonFactory) {
        if ($scope.pathError) return;
        CommonLogger.setTags(["ADMIN-TRANSLATION", "PER-FILE-TRANSLATION-STATUSES-CONTROLLER"]);
        $scope.page.title = "Per File Translation Status";

        $scope.allItemsSelected = false;

        $scope.page.filterScope = $scope;
        $scope.page.showPageableFilter = true;
        $scope.total = 0;

        var isTableEmpty = true;

        var pageRequest = contentRequestParameters.getParameters("translation");

        var loadData = function () {
            FileTranslationManager.statuses(pageRequest, function (data) {

                isTableEmpty = data.items.length == 0;
                $scope.total = data.itemsCount;

                $scope.fileTranslations = $.map(data.items, function(elem) {

                    var getStatus = function(file) {
                        if(file.translationSubmitted) {
                            if(file.translationError) {
                                return "Translation Error";
                            } else if(file.translationResultsAvailable) {
                                return "Success";
                            } else if(file.metadataAvailable) {
                                return "Success (Metadata Only)";
                            } else {
                                return "In Progress...";
                            }
                        } else {
                            return "Not Translated";
                        }
                    };

                    return {id: elem.id,
                        name: elem.name,
                        labName: elem.labName,
                        owner: elem.owner,
                        instrumentName: elem.instrumentName,
                        status: getStatus(elem),
                        usedInExperiment: elem.usedInExperiment ? "Yes" : "No"
                    };
                });
                startWatch($scope, switchRetranslateButton);
            });
        };



        function startWatch($scope, switchRetranslateButton) {
            $scope.$watch(function ($scope) {
                //watch the number of "selected" items
                var selected = $.grep($scope.fileTranslations, function (item) {
                    return item.selected
                });
                return selected.length;
            }, function (newVal) {
                switchRetranslateButton(newVal);
                $scope.allItemsSelected = newVal == $scope.fileTranslations.length;
            })
        }

        var retranslateSelected = function(metadataOnly) {
            var filesIds = {"files": [],"metadataOnly": metadataOnly};
            angular.forEach($scope.fileTranslations, function(item) {
                if(item.selected) this.files.push(item.id);
            }, filesIds);

            FileTranslationManager.reTranslateSelected(filesIds,loadData);
        };


        var switchRetranslateButton = function (enabled) {

            var button = new DashboardButton(0, "retranslate", "Retranslate Selected Files", "retranslate-selected");
            button.display = enabled;
            button.onClickHandler = function () {
                $scope.metadataOnly = false;
                $scope.retranslateSelectedPopup = new RetranslateSelectedPopup("#retranslate-selected-confirmation", retranslateSelected);
                $scope.retranslateSelectedPopup.showPopup();
            };

            DashboardButtonFactory.count(enabled);
            DashboardButtonFactory.origin("translationPerFile");
            DashboardButtonFactory.put(button);
        };

        function createReTranslateAllButton() {
            var button = new DashboardButton(10, "retranslate", "Retranslate All Files", "retranslate", "retranslate-btn");
            button.display = true;
            button.onClickHandler = function () {
                $scope.metadataOnly = false;
                $scope.retranslateSelectedPopup = new RetranslateSelectedPopup("#retranslate-all-confirmation", function (metadataOnly) {
                    FileTranslationManager.reTranslateAll({"metadataOnly":metadataOnly});
                });
                $scope.retranslateSelectedPopup.showPopup();
            };
            DashboardGlobalButtonFactory.put(button);
        }
        createReTranslateAllButton();

        $scope.isTableEmpty = function () {
            return isTableEmpty;
        };

        $scope.getEmptyTableMessage = function () {
            return "There are no files";
        };

        loadData();

        $scope.selectAll = function (selectAll, items) {
            angular.forEach(items, function (item) {
                item.selected = selectAll;
            });
        };

        $scope.changeSelection = function (item) {
            item.selected = !item.selected;
        };

        var RetranslateSelectedPopup = function(selector,retranslateCallback) {
            this.popupSelector = selector;
            this.retranslateCallback = retranslateCallback;
        };

        RetranslateSelectedPopup.prototype.showPopup = function() {
            $(this.popupSelector).modal("show");
        };
        RetranslateSelectedPopup.prototype.hidePopup = function () {
            $(this.popupSelector).modal("hide");
        };

        RetranslateSelectedPopup.prototype.retranslate = function(metadataOnly) {
            this.retranslateCallback(metadataOnly);
            this.hidePopup();
        };
    });

function startWatch($scope, switchRetranslateButton) {
    $scope.$watch(function ($scope) {
        //watch the number of "selected" items
        var selected = $.grep($scope.experimentTranslations, function (item) {
            return item.selected
        });
        return selected.length;
    }, function (newVal) {
        switchRetranslateButton(newVal);
        $scope.allItemsSelected = newVal == $scope.experimentTranslations.length;
    })
}







