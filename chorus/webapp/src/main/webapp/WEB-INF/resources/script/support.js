angular.module("support-page", ["security-front", "error-catcher", "current-year", "features-back", "header"])
    .config(function($routeProvider){
        $routeProvider
            .when("/questions", {redirectTo: "questions/faq"})
            .when("/videos", {redirectTo: "videos/relevant"})
            .when("/documentation", {redirectTo: "documentation/relevant"})
            .when("/message", {controller: "message-controller", templateUrl: "../pages/support/message.html"})
            .when("/questions/:category", {controller: "questions-controller", templateUrl: "../pages/support/questions.html"})
            .when("/videos/:category", {controller: "videos-controller", templateUrl: "../pages/support/videos.html"})
            .when("/documentation/:category", {controller: "documentation-controller", templateUrl: "../pages/support/documentation.html"})
            .when("/search", {controller: "search-controller", templateUrl: "../pages/support/search.html"})
            .otherwise({redirectTo: "questions/faq"});
    })
    .service("Questions", function(){
        return [
            {
                title: "I am a new user who wants to upload data and create projects and studies.  What should I do first?",
                content: "First, you should request Program (laboratory) membership or creation if you have not already done so. Next, you can either create a project or an instrument. These steps have no prerequisites other than belonging to a Program(laboratory) with mass spectrometers. Registering an instrument will let you upload .raw files from that instrument. Once you have created a project and have uploaded .raw data to the Workspace, you can create Studies(experiments) and organize them into your created projects. You can't create an Study (experiment) without specifying an existing project where you want the study(experiment) to belong.",
                category: "projects",
                availableForCelgene: true,
                isMostRelevant: true
            },
            {
                title: "Do I need to join a Program to use PDC Workspace?",
                content: "Yes, you need to be authorized to use the workspace. Register with a Google account or NIH eRA Commons account and select a program from the available programs listed. Once approved, you will be able to login using the method you chose to register.",
                category: "accounts",
                availableForCelgene: true,
                isMostRelevant: true
            },
            {
                title: "Can I share my data files with another person who does not have an account on PDC Workspace?",
                content: "Yes. You can create a permanent download link by clicking the “public link” button to the right of the study's name. This link can be shared on a public website or sent via email. The files will be downloaded as a .zip file. Note: Download is howevevr disabled for the MVP phase of the product",
                category: "groups",
                availableForCelgene: true,
                isMostRelevant: true
            },
            {
                title: "What kind of files can I upload to PDC Workspace?",
                content: "PDC Workspace accepts files with the .raw extension. These files come straight from the spectrometers and contain intensity and retention time data for each detected peak.",
                category: "upload",
                availableForCelgene: true,
                isMostRelevant: true
            },
            {
                title: "I uploaded some data files to PDC Workspace and I can't find them on the Create Study (experiment) screens. Am I doing something wrong?",
                content: "Most likely, the model organism or the instrument used to generate the data were incorrectly filled on either the Create Study or the Upload Files pages. These fields must match to add data files to your study.",
                category: "experiments",
                availableForCelgene: true,
                isMostRelevant: true
            },
            {
                title: "What are the fees for using PDC Workspace?",
                content: "There is no fee to use PDC Workspace, however the user needs to be registred to one of the available programs. This is a minimum viable product with a set of features to satisfy early adopters. To limit operational costs the downloads are disabled and uploads are limited to 2GB per program and the data will not be stored beyond the testing perios.",
                category: "billing",
                availableForCelgene: false,
                isMostRelevant: true
            },
            {
                title: "How will my account be billed?",
                content:  'There is no fee to use PDC Workspace during the MVP phase.',
                category: "billing",
                availableForCelgene: false,
                isMostRelevant: true
            }
        ];
    })
    .service("Videos", function(){
        var videoLists = {};
        //relevant playlist
        var playlistId = "PL1pRI26VpljPqC6Bc0jTyD0Uu6vHxy522";
        var APIKey = "AIzaSyB4FWtskLSR7Owq-EkwC9YITQedgen9NpE";//key for Andrii's work email, cause without it exceed limit error is appearing
        var url = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&&playlistId=" + playlistId + "&key=" + APIKey;

        //next playlist...
        return {
            load: function (successHandler){
                $.getJSON(url, function (data) {
                    videoLists.relevant = data.items;
                    for (var i = 0; i < videoLists.relevant.length; i++) {
                        if (videoLists.relevant[i].snippet.title == "Create New Experiment") {
                            videoLists.relevant[i].snippet.title = "Create New Study (Experiment)";
                        }
                    }
                    successHandler(videoLists);
                });

            }
        };
    })
    .service("Documentation", function(){
        return [];
    })
    .controller("questions-controller", function($scope, $routeParams, $timeout){
        var categoryFilter = $routeParams.category;

        $scope.questions = filterByCategory(categoryFilter, $scope.allQuestions);

        function filterByCategory (category, items){
            if(category == "faq"){
                return items.filter(function(item){
                    return item.isMostRelevant;
                })
            }else{
                return items.filter(function(item){
                    return item.category == category;
                })
            }

        }
    })
    .controller("videos-controller",function($scope, $rootScope, $routeParams, SupportConstants){

        var categoryFilter = $routeParams.category;
        updateVideos();

        $scope.$on(SupportConstants.UPDATE_VIDEOS, updateVideos);

        function updateVideos() {
            $scope.videos = $scope.allVideos[categoryFilter] || [];
        }

        $scope.showVideo = function(index) {
            $rootScope.video = $scope.allVideos[categoryFilter][index];
            setTimeout(function(){$scope.$apply();});
            setTimeout(function(){
                $("#preview-video").modal("show");
            });
        };
    })
    .controller("documentation-controller", function($scope){

    })
    .controller("message-controller", function($scope){

    })
    .controller("search-controller", function($scope, $rootScope){
        var items = {
            videos: [],
            faqs: [],
            documentation: []
        };
        $scope.lastSearchQuery = $scope.searchQuery;

        function getVideos(query){
            var videos = [];
            for(var name in $scope.allVideos){
                /*skip it, as it consists of duplicates

                 if(name == "relevant"){
                    continue;
                } */
                $.each($scope.allVideos[name], function(i, item){
                    if(item.video.title.toLowerCase().indexOf(query) >= 0){
                        videos.push(item)
                    }
                })
            }
            return videos;
        }

        function getQuestions(query){
            var questions = [];
            $.each($scope.allQuestions, function(i, item){
                if(item.title.toLowerCase().indexOf(query) >= 0
                    || item.content.toLowerCase().indexOf(query) >= 0){
                    questions.push(item)
                }
            });
            return questions;
        }

        function getDocumentation(query){
            var docs = [];
            $.each($scope.allDocumentation, function(i, item){
                if(item.title.toLowerCase().indexOf(query) >= 0
                    || item.content.toLowerCase().indexOf(query) >= 0){
                    docs.push(item)
                }
            });
            return docs;
        }

        $scope.showVideo = function(index) {
            $rootScope.video = $scope.results.videos[index];
            setTimeout(function(){$scope.$apply();});
            setTimeout(function(){
                $("#preview-video").modal("show");
            });
        };

        var q = $scope.lastSearchQuery.toLowerCase();
        items.videos = getVideos(q);
        items.faqs = getQuestions(q);
        items.documentation = getDocumentation(q);

        $scope.results = items;

        setTimeout(function(){
            $(".search-results").highlight($scope.lastSearchQuery)
        })

    })
    .controller("support-controller", function ($scope, $window, $rootScope, $location, $timeout, Questions, Videos,
                                                Documentation, Features, SupportConstants) {
        $scope.forumProperties = {};
        $scope.allQuestions = {};
        $scope.allVideos = {};
        $scope.allDocumentation = Documentation;
        $scope.searchQuery = "";
        $scope.isSearchResult = false;

        init();

        function init() {

            Features.getForumProperties(function (response) {
                $scope.forumProperties = response;
            });

            Features.getPrivateInstallProperties(function (privateInstallProperties) {
                $scope.allQuestions = $.grep(Questions, function (question) {
                    return !privateInstallProperties.enabled || question.availableForCelgene;
                });

                Videos.load(function (videos) {
                    $.each(videos, function (category, list) {
                        $scope.allVideos[category] = $.grep(list, function (video) {
                            return !privateInstallProperties.enabled ||
                                SupportConstants.hideOnPrivateInstallationVideoIds.indexOf(video.id) === -1;
                        });
                    });
                    $scope.$broadcast(SupportConstants.UPDATE_VIDEOS);
                    $timeout(function () {
                        $scope.$apply();
                    }, 0);
                });
            });
        }


        $scope.toggleOpened = function(e){
            var li = $(e.target).closest("li");
            li.toggleClass("opened");
            li.find(".hide").slideToggle(300);
            return false;
        };

        $scope.getActiveTab = function(){
            var path = $location.path();
            var pathParts = path.split("/");
            return pathParts[1];
        };

        $scope.isLinkActive = function(kind, category){
            var pathParts = $location.path().split("/");
            var k = pathParts[1];
            var c = pathParts[2];
            return kind == k && category == c;
        };

        $scope.$on("$routeChangeSuccess", function() {
            setTimeout(function() {
                $(".scroll-area").scrollTop(0);
            },0);

            var pathParts = $location.path().split("/");
            var flag = $scope.isSearchResult;
            $scope.isSearchResult = pathParts[1] == "search";
            if(!$scope.isSearchResult && flag != $scope.isSearchResult){
                $scope.searchQuery = "";
            }
        });

        $scope.search = function(){
            if($scope.searchQuery.trim().length == 0){
                $scope.searchQuery = "";
                return;
            }
            $location.url("search?value=" + $scope.searchQuery);
        };

        $scope.closeVideo = function() {
            delete $rootScope.video;
            setTimeout( function(){$scope.$apply();});
        };

    })
    .directive("onEnter", function () {
        //inspired by https://groups.google.com/forum/#!msg/angular/tv4Nl1HouOw/i0MMmfkMrbUJ
        return function (scope, element, attrs) {
                element.bind("keydown keypress", function (event) {
                    if (event.which === 13) {
                        scope.$apply(function () {
                            scope.$eval(attrs.onEnter);
                        });

                        event.preventDefault();
                    }
                });
            };
    })
    .factory("SupportConstants", function () {
        return {
            UPDATE_VIDEOS: "UPDATE_VIDEOS",
            hideOnPrivateInstallationVideoIds: ["UEwxcFJJMjZWcGxqUHFDNkJjMGpUeUQwVXU2dkh4eTUyMi41MzJCQjBCNDIyRkJDN0VD"]
        };
    });
