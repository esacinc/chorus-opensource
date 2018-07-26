angular.module("laboratoriesControllers", ["laboratories", "validators", "front-end", "error-catcher"]).
    controller("laboratories",function ($scope, $rootScope, $location, $routeParams, Laboratories, contentRequestParameters, laboratoriesExpandMenu) {
        if($scope.pathError) return;
        CommonLogger.setTags(["LABORATORIES", "LABORATORIES-CONTROLLER"]);
        $scope.page.title = "programs";
        $scope.page.showFilter = true;
        var isTableEmpty = false;

        var request = contentRequestParameters.getParameters("laboratories");
        laboratoriesExpandMenu($scope);

        $scope.sorting = {};
        $scope.sorting.reverse = !request.asc;
        $scope.sorting.field = request.sortingField;
        $scope.page.filter = request.filterQuery;
        $scope.laboratories = Laboratories.query({filter:$routeParams.filter}, function () {
            isTableEmpty = $scope.laboratories.length == 0;
        });
        $scope.filter = $routeParams.filter;
        $scope.page.subtitle = $scope.$eval("filter | filterToString");


        $scope.showLaboratoryDetails = function (laboratory) {
            $rootScope.dialogReturnUrl = $location.url();
            $location.path("/laboratories/" + $routeParams.filter + "/" + laboratory.id);
        };

        $scope.isTableEmpty = function () {
            return isTableEmpty;
        };

        $scope.getEmptyTableMessage = function () {
            return "There are no programs";
        };
    }).
    controller("newLaboratory",function ($scope, $location, Laboratories) {
        if($scope.pathError) return;
        CommonLogger.setTags(["LABORATORIES", "NEW-LABORATORY-CONTROLLER"]);
        $scope.showCreateDialog = true;
        $scope.laboratory = {};

        $scope.save = function () {
            Laboratories.save($scope.laboratory, function () {
                CommonLogger.log("Saved");
                setTimeout(function () {
                    $(".modal").modal("hide");
                }, 0);
            });
        };
    }).
    controller("laboratoryDetails", function ($scope, $location, $routeParams, Laboratories, LaboratoryDetails) {
        if($scope.pathError) return;
        CommonLogger.setTags(["LABORATORIES", "LABORATORY-DETAILS-CONTROLLER"]);
        $scope.page.title = "Program Details";
        Laboratories.get({filter:"details", id:$routeParams.id}, function (laboratory) {
            $scope.details = laboratory;
        });


        $scope.save = function () {
            var laboratory = {};
            laboratory.id = $scope.details.id;
            laboratory.institutionUrl = $scope.details.institutionUrl;
            laboratory.name = $scope.details.name;
            laboratory.headFirstName = $scope.details.headFirstName;
            laboratory.headLastName = $scope.details.headLastName;
            laboratory.headEmail = $scope.details.headEmail;


            LaboratoryDetails.update(laboratory, function () {
                CommonLogger.log("Group Saved");
                setTimeout(function () {
                    $(".modal").modal("hide");
                }, 0);
            });
        };
    })
    .directive("laboratoryDetails", detailsLink({"title":"Show Program Details", "dataTarget":"#laboratoryDetails"}))
    .factory("laboratoriesExpandMenu", function(Laboratories){
        return initExpandMenu(function(laboratory){
            Laboratories.get({filter:"details", id:laboratory.id}, function (details) {
                laboratory.details = details;
            });
        })
    });