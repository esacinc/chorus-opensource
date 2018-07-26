angular.module("experiments-front", ["mixins", "experiments-back", "protein-search-back", "projects", "users", "ui.directives",
    "ui", "modals", "breadcrumbs", "js-upload", "security-front", "security-back", "front-end", "downloader", "error-catcher",
    "dashboard-common-directives", "enums"])
    .config(function ($provide) {
        $provide.decorator("orderByFilter", function ($delegate) {
            var states = {};
            return function (arr, predicate, reverse, ignore, id) {
                if (ignore) {
                    if(states[id]) {
                        return states[id];
                    }
                    return arr;
                }
                var order = $delegate.apply(null, arguments);
                if (id) {
                    states[id] = order;
                }
                return order;
            }
        });
    })
    .factory("initWizard", function ($location, $route, $rootScope, ExperimentSpecies, ExperimentInstrumentModels, Projects,
                                     ProjectDetails, ExperimentInstruments, ExperimentFiles, PagedExperimentFiles, ExperimentDetailsFiles,
                                     Experiments, ExperimentAttachments, startNewUpload, Laboratories, ExperimentShortDetails,
                                     formatExperimentProject, formatInstrument, ExperimentDetails, Instruments, $filter, LabFeatures,
                                     reversedSorting, experimentByFiles, Security, ExperimentTranslation, ExperimentAnnotationAttachment,
                                     translateExperimentConfirmation, InstrumentTechnologyTypes, InstrumentVendors, InstrumentStudyType) {

        function init($scope, experiment, viewMode, originalExperimentID) {
            $scope.analysisStepConfiguration = {};
            $scope.fileToPrepAssignmentStepConfiguration = {};
            $scope.sampleAssignmentStepConfiguration = {};
            $scope.sampleToFactorsStepConfiguration = {};
            $scope.expanded = false;
            $scope.viewMode = viewMode ? viewMode : false;
            $scope.experiment = experiment;
            $scope.originalExperimentCopy = JSON.parse(JSON.stringify(experiment));
            $scope.projects = [];
            $scope.species = [];
            $scope.instruments = [];
            $scope.currentInstrumentModel = {};
            $scope.labs = [];
            $scope.items = [];
            $scope.files = [];
            $scope.selectedFiles = {};
            $scope.selectors = {};
            $scope.instrumentModels = [];
            $scope.workflowTypes = [];
            $scope.lockMasses = experiment.lockMasses || [];
            $scope.labsWithEnabledBilling = [];
            $scope.page.changeableExperimentsColumns = true;
            $scope.existingAnnotationAttachmentModel = {
                experiment: experiment.id,
                existingAttachments: [],
                removedAttachments: []
            };

            $scope.filesFilter = {
                enabled : false,
                fromEnabled : false,
                toEnabled : false,
                containsEnabled : false,
                fromDate : "",
                toDate : "",
                contains : ""
            };
            $scope.experimentItems = [];
            $scope.initializedRestriction = angular.copy($scope.experiment.restriction);
            $scope.filesInfiniteScrollDisable = true;
            $scope.loadNextFilesPage = null;
            $scope.totalFilePages = 0;
            $scope.currentFilesPage = 1;
            $scope.filesLoaded = false;
            $scope.preProcessBackStep = null;
            $scope.shouldReloadFiles = false;
            $scope.finish = finish;
            $scope.back = back;
            $scope.isMs = isMs;
            $scope.shouldSkipFourthStep = shouldSkipFourthStep;
            $scope.shouldSkipFifthStep = shouldSkipFifthStep;

            var sampleNameToTypes = {}; // handles duplicates when selection is specified

            updateExperimentExperimentModelForView($scope.experiment);

            $scope.$on("analysisStepConfiguration.initialized", onAnalysisStepComponentInitialized);

            if (viewMode)
                $scope.confirmSelectAllLabs = !experiment.lab;

            $scope.returnUrl = $rootScope.returnUrl;
            /*var url = $location.path();
             $scope.returnUrl = $scope.returnUrl.lastIndexOf("/files") != -1 ? $scope.returnUrl : url.substring(0, url.lastIndexOf("/"));*/

            function allowedExtensionsFn() {
                if ($scope.currentInstrumentModel &&
                    $scope.currentInstrumentModel.name &&
                    $scope.currentInstrumentModel.name.indexOf("Bruker") == 0) {
                    return [".mir"];
                } else {
                    return [];
                }
            }

            /*** Experiment Attachments ***/
            AttachmentsHelper.commonSetup(
                $scope,
                experiment.id,
                "#experiment-modal-dialog",
                ExperimentAttachments,
                startNewUpload,
                "../attachments/experiment/download/",
                !$scope.viewMode, allowedExtensionsFn,
                {
                    displayDragAndDropAreaOnlyIfAreaPresented: true
                }
            );

            var util = {
                refreshFilesFn: refreshFilesFn,
                getSelectedFiles: getSelectedFiles
            };

            if(!$scope.createMode && $scope.experiment.id || originalExperimentID){ //if edit or copy experiment, fetch experiment files
                var experimentIDtoFetchSelectedFiles = $scope.experiment.id ? $scope.experiment.id : originalExperimentID;
                ExperimentDetailsFiles.byExperiment({experiment: experimentIDtoFetchSelectedFiles}, function (files) {
                    $scope.experimentItems = files;
                });
            } else if($scope.experiment.files && $scope.experiment.files.length > 0){// if experiment created from selected files
                $scope.experimentItems = $scope.experiment.files;
            }

            function shouldSkipFourthStep() {
                return !$scope.isMs() || !$scope.experiment.is2dLc;
            }

            function shouldSkipFifthStep() {
                return !$scope.isMs() || !$scope.experiment.labelType;
            }

            function refreshFilesFn(filesRequestHandlerFn, params) {
                return function () {
                    if (!$scope.instrumentModels || $scope.instrumentModels.length == 0) {
                        CommonLogger.log("instrumentModels is not defined yet");
                        return;
                    }
                    var instrument = $scope.experiment.restriction.instrument;
                    var instrumentModel = $scope.experiment.restriction.instrumentModel;
                    var specie = $scope.experiment.info.specie;

                    $scope.currentInstrumentModel = $.grep($scope.instrumentModels, function (instr) {
                        return instr.id == instrumentModel;
                    })[0];

                    AttachmentsHelper.updateExtensionsLabelSupported($scope, allowedExtensionsFn);

                    $scope.loadNextFilesPage = loadNextFilesPage;
                    if (!instrument && $scope.experimentItems.length > 0) {
                        CommonLogger.log("It's impossible to create experiment with selected files and without instrument");
                        return;
                    }
                    if (((instrument && instrumentModel) || instrumentModel) && specie) {
                        $scope.currentFilesPage = 1;// if new instrument or instrumentModel selected need reload files
                        $scope.filesLoaded = false;
                        loadFilesPage();
                    }
                    CommonLogger.log("Loading files for instrument model " + instrumentModel + ", instrument " + instrument + ", specie " + specie);

                    function loadFilesPage() {
                        var instrumentParam = isNumber(instrument) && instrument != -1 ? instrument : "";
                        var modelParam = instrumentModel ? instrumentModel : "";
                        var labParam = $scope.experiment.lab > 0 ? experiment.lab : "";
                        var fromDateFilterQueryParam = ($scope.filesFilter.enabled && $scope.filesFilter.fromEnabled && $scope.filesFilter.fromDate) ?
                            $scope.filesFilter.fromDate : "";
                        var toDateFilterQueryParam = ($scope.filesFilter.enabled && $scope.filesFilter.toEnabled && $scope.filesFilter.toDate) ?
                            $scope.filesFilter.toDate : "";
                        var filterQueryParam = ($scope.filesFilter.enabled && $scope.filesFilter.containsEnabled && $scope.filesFilter.contains) ?
                            $scope.filesFilter.contains : "";

                        var itemPerPage = params && params.itemsPerPage ? params.itemsPerPage : ExperimentsConstants.FILES_ITEMS_PER_PAGE;

                            PagedExperimentFiles.get({
                            page: $scope.currentFilesPage,
                            items: itemPerPage,
                            sortingField: "uploadDate",
                            asc: false,
                            filterQuery: filterQueryParam,
                            fromDateFilterQuery: fromDateFilterQueryParam,
                            toDateFilterQuery: toDateFilterQueryParam,
                            instrument: instrumentParam,
                            model: modelParam,
                            lab: labParam,
                            specie: specie
                        }, receiveFilesPage);
                    }

                    function loadNextFilesPage(){
                        $scope.currentFilesPage += 1;
                        loadFilesPage();
                    }

                    function setFilesDefaults(allFiles, newFiles) {
                        angular.forEach(allFiles, function (file) {
                            file.factors = {};
                            file.annotations = {};
                        });
                        if (filesRequestHandlerFn) {
                            filesRequestHandlerFn(allFiles, newFiles);
                        }

                        $scope.selectedFilesLength = getSelectedFiles().length;
                    }

                    function receiveFilesPage(files){
                        $scope.ignoreFilesOrdering = false;
                        $scope.totalFilePages = files.totalPages;
                        $scope.currentFilesPage = files.pageNumber + 1;
                        $scope.filesInfiniteScrollDisable = $scope.currentFilesPage > $scope.totalFilePages;
                        $scope.totalFileItems = files.itemsCount;

                        var selectedFiles = $scope.experimentItems;
                        if ($scope.initializedRestriction.instrument != instrument ||
                            $scope.initializedRestriction.instrumentModel != instrumentModel ||
                            $scope.filesFilter.enabled) {

                            selectedFiles = [];
                        }
                        var newFiles = files.items;
                        var allFiles = files.pageNumber == 0 ? concatItemsUnique(selectedFiles, newFiles) : newFiles;
                        $scope.specifySelectedFiles(allFiles);

                        $scope.filesLoaded = true;

                        // mark files as not selected
                        for (var i = 0; i < allFiles.length; i++) {
                            if (allFiles[i] && typeof allFiles[i].selected == "undefined") {
                                allFiles[i].selected = false;
                            }
                        }
                        $scope.files = files.pageNumber == 0 ? allFiles : concatItemsUnique($scope.files, allFiles);
                        setFilesDefaults($scope.files, newFiles);
                    }

                    function concatItemsUnique(oldItems, newItems){
                        var result = oldItems.concat([]);
                        angular.forEach(newItems, function (file) {
                            var found = $.grep(oldItems, function (oldFile) {
                                return oldFile.id == file.id;
                            });
                            if(found.length <= 0){
                                result.push(file);
                            }
                        });
                        return result;
                    }

                };
            }

            function getSelectedFiles() {
                return $scope.$eval("files | selected");
            }


            function finish() {
                $scope.conditions = getConditions(excludeNameDuplicates($scope.experiment.samples), $scope.experiment.factors);
                $scope.step = 7;
                $scope.selectedFilesLength = getSelectedFiles().length;
            }

            function back(){

                if($scope.preProcessBackStep){
                    $scope.preProcessBackStep($scope.step);
                }
                $scope.step--;
                preProcessBackStep($scope.step);
            }

            function preProcessBackStep(step) {
                switch (step) {
                    case 5: {
                        var labeledMs = $scope.experiment.labelType && isMs();
                        var multiplexingNGS = $scope.experiment.ngsRelatedInfo.multiplexing && isNGS();
                        if(!labeledMs && !multiplexingNGS) {
                            back();
                        }
                        break;
                    }
                    case 4:
                        var is2dlcMs = isMs() && $scope.experiment.is2dLc;
                        if(!is2dlcMs) {
                            back();
                        }
                        break;
                }
            }

            function isMs() {
                return techTypeIs(InstrumentStudyType.MS);
            }

            function isNGS() {
                return techTypeIs(InstrumentStudyType.NG);
            }

            function techTypeIs(type) {
                if($scope.experiment.restriction) {
                    return $scope.experiment.restriction.technologyTypeValue === type;
                }
            }

            $scope.next = next;
            function next(isInvalid) {
                $scope.buttonPressed = true;
                if (isInvalid) return;
                $scope.buttonPressed = false;

                postProcessStep($scope.step);
                $scope.step++;
                preProcessStep($scope.step);
            }

            $scope.specifySelectedFiles = function (files) {
                angular.forEach(files, function (file) {
                    if ($scope.experiment.files) {
                        var foundInExperiment = $.grep($scope.experiment.files, function (fileInExperiment) {
                            return fileInExperiment.id == file.id;
                        });
                        if (foundInExperiment.length > 0) {
                            file.selected = true;
                            for (var i = 0; i < $scope.experiment.factors.length; i++) {
                                $scope.experiment.factors[i].id = i;
                            }
                        }
                    }
                });
            };

            $scope.findById = function (objects, id) {
                if (!objects) {
                    return;
                }
                return jQuery.grep(objects, function (elem) {
                    return (elem.id == id);
                })[0];
            };

            var _findNameById = function (list, id) {
                if (!list) {
                    return "";
                }
                var matchingItems = $.grep(list, function (item) {
                    return (item.id == id);
                });
                if (!matchingItems.length || matchingItems.length === 0) {
                    return "";
                }
                return matchingItems[0].name;
            };

            $scope.getLabName = function (labId) {
                if (labId === null || labId === -1) {
                    return "Look through all labs";
                }
                return _findNameById($scope.labs, labId) || $scope.experiment.labName || "";
            };

            $scope.getBillLabLabName = function (labId) {
                if (labId === null) {
                    return "You are not a member of any lab. Due to absense of lab billing information " +
                        "the translation and analysis will not be available for this experiment";
                }
                return _findNameById($scope.labs, labId) || $scope.experiment.labName || "";
            };

            $scope.getSpecieName = function (specieId) {
                return _findNameById($scope.species, specieId)
            };

            $scope.getInstrumentName = function (instrumentId) {
                return _findNameById($scope.instruments, instrumentId)
            };

            $scope.getInstrumentModelName = function (modelId) {
                return _findNameById($scope.instrumentModels, modelId);
            };
            return util;

            function sortFn(a, b) {
                if (a === b) return 0;

                var aIsString = isNaN(a);
                var bIsString = isNaN(b);
                if (aIsString) {
                    if (bIsString) {
                        return a.toLocaleLowerCase().localeCompare(b.toLocaleLowerCase());
                    } else {
                        return 1;
                    }
                } else {
                    if (bIsString) {
                        return -1
                    } else {
                        return parseInt(a) > parseInt(b) ? 1 : -1;
                    }
                }
            }

            function postProcessStep(step) {
                switch (step) {
                    case 2:// initializing labels and experiment type
                        if ($scope.analysisStepConfiguration) {
                            var selected = $scope.analysisStepConfiguration.api.getSelected();

                            var instrumentChanged =  $scope.experiment.restriction.instrument != selected.instrument;

                            $scope.experiment.is2dLc = selected.is2dLc;
                            $scope.experiment.mixedSamplesCount = selected.mixedSampleType;
                            $scope.experiment.channelsCount = selected.channelsCount;
                            $scope.experiment.labelType = selected.selectedLabelType;
                            $scope.experiment.groupSpecificParametersType = selected.groupSpecificParametersType;
                            $scope.experiment.reporterMassTol = selected.reporterMassTol;
                            $scope.experiment.filterByPIFEnabled = selected.filterByPIFEnabled;
                            $scope.experiment.minReporterPIF = selected.minReporterPIF;
                            $scope.experiment.minBasePeakRatio = selected.minBasePeakRatio;
                            $scope.experiment.minReporterFraction = selected.minReporterFraction;
                            $scope.experiment.restriction.instrument = selected.instrument;
                            $scope.experiment.restriction.instrumentModel = selected.instrumentModel;
                            $scope.instrumentModels = $scope.analysisStepConfiguration.api.getInstrumentModels();
                            $scope.instruments = $scope.analysisStepConfiguration.api.getInstruments();
                            $scope.experiment.ngsRelatedInfo = selected.ngsRelatedInfo;

                            var shouldReloadFiles = $scope.shouldReloadFiles || instrumentChanged;
                            if(shouldReloadFiles) {
                                util.refreshFilesFn()();
                                $scope.shouldReloadFiles = false;
                            }
                        }
                        break;
                    case 4: // when prep sample is assigned to file
                        $scope.experiment.files = $scope.fileToPrepAssignmentStepConfiguration.api.getSelected();
                        // specify selected files filled with prepared sample name and fraction number
                        var preparedSamplesMap = {};
                        $($scope.experiment.files).each(function (i, file) {
                            preparedSamplesMap[file.preparedSampleName] = {name: file.preparedSampleName, samples: {}};
                        });
                        //preserve bio samples of prepared sample
                        var oldPrepSamplesByNameMap = {};
                        $($scope.experiment.preparedSamples).each(function (i, prepSample) {
                            oldPrepSamplesByNameMap[prepSample.name] = prepSample;
                        });
                        var preparedSamplesList = [];

                        $.each(preparedSamplesMap, function (prepSampleName, preparedSample) {
                            if (oldPrepSamplesByNameMap[prepSampleName]) {
                                preparedSample.samples = oldPrepSamplesByNameMap[prepSampleName].samples;
                            }
                            preparedSamplesList.push(preparedSample);
                        });

                        preparedSamplesList.sort(function(a, b){
                            return a.name.toLocaleLowerCase().localeCompare(b.name.toLocaleLowerCase());
                        });

                        $scope.experiment.preparedSamples = preparedSamplesList;
                        break;
                    case 5:// when bio samples assigned to prepared sample
                        //preserve factor values
                        var sampleNameToFactorValuesMap = {};
                        $($scope.experiment.samples).each(function (i, sample) {
                            sampleNameToFactorValuesMap[sample.name] = sample.factorValues;
                        });
                        $scope.experiment.samples = $scope.sampleAssignmentStepConfiguration.api.getSelected();
                        // specify selected samples from component and retain old factorValues
                        $($scope.experiment.samples).each(function (i, sample) {
                            sample.factorValues = sampleNameToFactorValuesMap[sample.name];
                        });

                        $scope.experiment.samples.sort(function(a,b){
                            return sortFn(a.name, b.name);
                        });

                        break;
                    case 6:// factors assigned to bio samples
                        var selected = $scope.sampleToFactorsStepConfiguration.api.getSelected();
                        $scope.experiment.samples = composeSamplesWithNameDuplicates(selected.samples); // filled with factorValues
                        $scope.experiment.factors = selected.factors;
                        break;
                }
            }

            function preProcessStep(step) {
                switch (step) {
                    case 4: // files to prepared sample
                        var selectionToSpecify = {
                            is2dLc: $scope.experiment.is2dLc,
                            mixedSamplesCount: $scope.experiment.mixedSamplesCount,
                            channelsCount: $scope.experiment.channelsCount,
                            files: function prepareFilesForPreparedSample() {
                                var exFileIdToFileMap = {};
                                $($scope.experiment.files).each(function (i, file) {
                                    exFileIdToFileMap[file.id] = file;
                                });
                                var selectedFiles = util.getSelectedFiles();
                                $(selectedFiles).each(function (i, file) {
                                    if (!file.preparedSampleName) {
                                        file.preparedSampleName = null;
                                        // load data from experiment if editing
                                        var experimentFile = exFileIdToFileMap[file.id];
                                        if (experimentFile) {
                                            file.fractionNumber = experimentFile.fractionNumber;
                                            file.preparedSampleName = (experimentFile.preparedSample) ? experimentFile.preparedSample.name : null;
                                        }
                                    }
                                });
                                return selectedFiles;
                            }()
                        };
                        $scope.$broadcast($scope.fileToPrepAssignmentStepConfiguration.api.events.setSelected, selectionToSpecify);
                        var is2dlcMs = isMs() && $scope.experiment.is2dLc;
                        if(!is2dlcMs) {
                            next();
                        }
                        break;
                    case 5: // bio samples to prepared sample assignment
                        var selectionToSpecify = {
                            preparedSamples: $scope.experiment.preparedSamples,
                            mixedSamplesCount: $scope.experiment.mixedSamplesCount,
                            channelsCount: $scope.experiment.channelsCount,
                            labelType: $scope.experiment.labelType
                        };
                        $scope.$broadcast($scope.sampleAssignmentStepConfiguration.api.events.setSelected, selectionToSpecify);

                        var labeledMs = $scope.experiment.labelType && isMs();
                        var multiplexingNGS = $scope.experiment.ngsRelatedInfo.multiplexing && isNGS();
                        if(!labeledMs && !multiplexingNGS) {
                           next();
                        }
                        break;
                    case 6: // factors to bio samples
                        // load data from experiment if editing
                        var exSampleIdToSampleMap = {};
                        $($scope.originalExperimentCopy.samples).each(function (i, sample) {
                            exSampleIdToSampleMap[sample.name] = sample;
                        });
                        $($scope.experiment.samples).each(function (i, sample) {
                            if (!sample.factorValues) {
                                sample.factorValues = {};
                                var exSample = exSampleIdToSampleMap[sample.name];
                                if (exSample) {
                                    sample.factorValues = exSample.factorValues;
                                }
                            } else {
                                //transform to map if array
                                if (Array.isArray(sample.factorValues)) {
                                    var oldFactorValues = sample.factorValues;
                                    sample.factorValues = {};
                                    $(oldFactorValues).each(function (i, factorValue) {
                                        sample.factorValues[i] = factorValue;
                                    })
                                }

                            }
                        });
                        var selectionToSpecify = {
                            experimentId: $scope.experiment.id,
                            samples: excludeNameDuplicates($scope.experiment.samples),
                            factors: $scope.experiment.factors
                        };
                        $scope.$broadcast($scope.sampleToFactorsStepConfiguration.api.events.setSelected, selectionToSpecify);
                        break;
                    case 7:
                        $scope.finish();
                        break;
                }
            }

            function excludeNameDuplicates(samples){
                var samplesWithoutDuplicates = [];
                sampleNameToTypes = {};
                $(samples).each(function (i, sample) {
                    if (!sampleNameToTypes[sample.name]) { // initialize array and put unique sample names
                        sampleNameToTypes[sample.name] = [];
                        samplesWithoutDuplicates.push(sample);
                    }
                    sampleNameToTypes[sample.name].push(sample.type);
                });
                return samplesWithoutDuplicates;
            }
            function composeSamplesWithNameDuplicates(samples){
                var samplesWithDuplicates = [];
                $(samples).each(function (i, sample) {
                    if (!sampleNameToTypes[sample.name]) {
                        throw "Value should be in sampleNameToTypes map";
                    }
                    $(sampleNameToTypes[sample.name]).each(function (i, sampleType) {
                        var sampleCopy = angular.copy(sample);
                        sampleCopy.type = sampleType;
                        samplesWithDuplicates.push(sampleCopy);
                    });
                });
                return samplesWithDuplicates;
            }

            function getConditions(samples, factors) {
                function getCondition(sample) {
                    var factorsIndex = 0;
                    var conditionStr = "";
                    angular.forEach(sample.factorValues, function (factorValue) {
                        var factor = factors[factorsIndex];
                        var units = factor.isNumeric || factor.isNumeric == "true" ? "(" + factor.units + ")" : "";
                        conditionStr += factor.name + ":" + factorValue + units;
                        if (factorsIndex != (experiment.factors.length - 1)) {
                            conditionStr += ", ";
                        }
                        factorsIndex++;
                    });
                    return {sample: sample, value: conditionStr};
                }

                var conditions = [];
                angular.forEach(samples, function (sample) {
                    conditions.push(getCondition(sample));
                });
                return conditions;
            }

            function onAnalysisStepComponentInitialized() {
                if (!viewMode) {

                    var eventData = angular.copy($scope.experiment);
                    eventData.experimentType = $scope.experiment.info.experimentType;
                    eventData.labels = $scope.experiment.experimentLabels;
                    eventData.instrumentModel = $scope.experiment.restriction.instrumentModel;
                    eventData.instrumentType = $scope.experiment.restriction.instrumentType;
                    eventData.instrument = $scope.experiment.restriction.instrument;
                    eventData.originalExperimentCopy = $scope.originalExperimentCopy;
                    eventData.ngsRelatedInfo = eventData.ngsRelatedInfo || {};

                    $scope.$broadcast($scope.analysisStepConfiguration.api.events.SPECIFY_SELECTION, eventData);
                }
            }

            function getAnnotation(name, annotationValues) {
                return $.grep(annotationValues, function (item) {
                    return item.name == name;
                })[0];
            }
        }

        return {
            viewMode: function ($scope, experiment) {
                var util = init($scope, experiment, true);

                $scope.lockMasses = experiment.lockMasses;

                ProjectDetails.short({id: $scope.experiment.project}, function (project) {
                    $scope.projects = [project];
                });
                if ($scope.experiment.lab) {
                    $scope.labs = [Laboratories.get({filter: "short", id: $scope.experiment.lab})];
                }
                ExperimentSpecies.specie({id: $scope.experiment.info.specie}, function (specie) {
                    $scope.species = [specie];
                });

                function refreshFiles() {
                    function setFilesDefaults(files) { // hides already initialized function
                        $scope.files = files;
                        angular.forEach(files, function (file) {
                            file.factors = {};
                            file.annotations = {};
                        });
                        $scope.specifySelectedFiles($scope.files);
                        $scope.finish();
                    }

                    $scope.files = ExperimentDetails.query({id: $scope.experiment.id, path: "files"}, setFilesDefaults);

                }

                refreshFiles();
                $scope.finish();
            },
            createMode: function ($scope, experiment, loggedInUser, createMode, createFromFilesMode, selectAllLabs, originalExperimentID) {

                //set experiment.restriction.instrument to -1 (if needed) before
                //setting the experiment to $scope.experiment
                var experimentBeforeUpdate = angular.copy(experiment);
                var util;
                var currentRestriction = {};

                $scope.createMode = createMode;
                $scope.createFromFilesMode = createFromFilesMode || false;
                $scope.ignoreFilesOrdering = false;
                $scope.validation = {};
                $scope.showFileError = showFileError;
                $scope.bProteinSearchesWillBeRemoved = false;
                $scope.disableFinishButton = false;
                $scope.reversedSorting = reversedSorting($scope);
                $scope.expShortDetails = ExperimentShortDetails.query({filter: "my"});

                $scope.isExperimentNameDuplicated = isExperimentNameDuplicated;
                $scope.changeSelection = changeSelection;
                $scope.selectAll = selectAll;
                $scope.restoreFilesOrdering = restoreFilesOrdering;
                $scope.applyFilesFilter = applyFilesFilter;
                $scope.preProcessBackStep = preProcessBackStep;
                $scope.isFilesSelected = isFilesSelected;
                $scope.isFormInvalid = isFormInvalid;
                $scope.isAnyFormInvalid = isAnyFormInvalid;
                $scope.confirmAndFinish = confirmAndFinish;
                $scope.selectAllLabs = selectAllLabsFn;
                $scope.noInstrumentModels = noInstrumentModels;
                $scope.formatExperimentProject = formatExperimentProject;
                $scope.formatInstrument = formatInstrument;


                $scope.$watch("experiment.lab", onLabChange);
                $scope.$watch("experiment.restriction.technologyType", onTechnologyTypeChange);
                $scope.$watch("experiment.restriction.vendor", onVendorChange);
                $scope.$watch("experiment.restriction.instrument", onInstrumentChange, true);
                $scope.$watch("experiment.info.specie", onSpeciesChange);
                $scope.$watch("filesFilter.fromEnabled", onDisableFilesFilter);
                $scope.$watch("filesFilter.toEnabled", onDisableFilesFilter);
                $scope.$watch("filesFilter.containsEnabled", onDisableFilesFilter);

                activate();

                function activate() {
                    updateExperimentExperimentModelForView(experimentBeforeUpdate);
                    if (experimentBeforeUpdate.restriction.instrument == null) {
                        experimentBeforeUpdate.restriction.instrument = -1;
                    }

                    util = init($scope, experiment, false, originalExperimentID);

                    $scope.analysisStepConfiguration.createMode = createMode;

                    if (!createMode || createFromFilesMode) {
                        var instrument = experiment.restriction.instrument;
                        experiment.restriction.instrument = instrument ? instrument : -1;
                        $scope.confirmSelectAllLabs = !experiment.lab;
                    }

                    if (selectAllLabs || experimentBeforeUpdate.lab === null) {
                        $scope.experiment.lab = -1;
                    }

                    $scope.step = 1;

                    loadProjects();
                    loadExperimentSpecies();
                    loadLabs();
                    loadTechnologyTypes();
                }

                function loadProjects() {
                    Projects.query({filter: "allowedForWriting"}, function (projects) {
                        $scope.projects = projects;
                        if (!$scope.experiment.project) {
                            $scope.experiment.project = getDefaultOptionValue($scope.projects, $scope.experiment.project);
                        }
                    });
                }

                function loadExperimentSpecies() {
                    ExperimentSpecies.query(function (species) {
                        $scope.species = species;
                        if (!$scope.experiment.info.specie) {
                            $scope.experiment.info.specie = getDefaultOptionValue($scope.species, $scope.experiment.info.specie);
                        }
                    });
                }

                function loadLabs() {
                    Laboratories.labitems(function (labs) {
                        var labs = $.grep(labs, function (lab) {
                            return $.inArray(lab.id, loggedInUser.labs) != -1;
                        });

                        $scope.labsWithEnabledBilling = Security.userLabsWithEnabledFeature({feature: "billing"});

                        $scope.labs = labs;
                        if (!$scope.experiment.lab && !(!createMode && $scope.originalExperimentCopy.lab === null)) {
                            $scope.experiment.lab = getDefaultOptionValue($scope.labs, $scope.experiment.lab);
                        }
                    });
                }

                function loadTechnologyTypes() {
                    InstrumentTechnologyTypes.query(function (technologyTypes) {
                        $scope.technologyTypes = technologyTypes;
                        $scope.experiment.restriction.technologyType = getDefaultOptionValue($scope.technologyTypes, $scope.experiment.restriction.technologyType);
                    });
                }

                function onLabChange() {
                    updateRestriction();
                }

                function onVendorChange() {
                    updateRestriction();
                }

                function onSpeciesChange() {
                    updateRestriction();
                    $scope.shouldReloadFiles = true;
                }

                function updateRestriction() {
                    var allInfoIsPresent = $scope.experiment.lab && $scope.experiment.restriction.technologyType && $scope.experiment.restriction.vendor;

                    // vendor is dependant on other fields, no need to check lab and tech type
                    var vendorChanged = $scope.experiment.restriction.vendor != currentRestriction.vendor;
                    var labChanged = $scope.experiment.lab != currentRestriction.lab;
                    var techTypeValueChanged = $scope.experiment.restriction.technologyTypeValue != currentRestriction.technologyTypeValue;
                    var speciesChanged = currentRestriction.species != $scope.experiment.info.specie;
                    var somethingChanged = vendorChanged || labChanged || techTypeValueChanged || speciesChanged;

                    if(allInfoIsPresent && somethingChanged) {
                        var restriction = {
                            species: $scope.experiment.info.specie,
                            lab: $scope.experiment.lab,
                            technologyType: $scope.experiment.restriction.technologyType,
                            vendor: $scope.experiment.restriction.vendor,
                            technologyTypeValue:  $scope.experiment.restriction.technologyTypeValue
                        };
                        $scope.analysisStepConfiguration.restriction = angular.copy(restriction);
                        currentRestriction = restriction;
                    }
                }


                function onInstrumentChange() {
                    var validInstrumentSelected = parseInt($scope.experiment.restriction.instrument) > 0;
                    var experimentHasNoOwnLockMasses = !$scope.experiment.lockMasses;
                    if (validInstrumentSelected && experimentHasNoOwnLockMasses) {
                        Instruments.get({id: $scope.experiment.restriction.instrument}, function (response) {
                            $scope.lockMasses = response.details.lockMasses;
                        });
                    }
                }

                function reloadFiles(){
                    disableFilesFilter();
                    util.refreshFilesFn()();
                }

                function onTechnologyTypeChange(technologyType) {
                    if (technologyType && $scope.technologyTypes) {
                        InstrumentVendors.byTechnologyType({techType: technologyType}, function (vendors) {
                            $scope.vendors = vendors;

                            if ($scope.originalExperimentCopy && technologyType == $scope.originalExperimentCopy.restriction.technologyType) {
                                $scope.experiment.restriction.vendor = $scope.originalExperimentCopy.restriction.vendor;
                            } else {
                                $scope.experiment.restriction.vendor = getDefaultOptionValue($scope.vendors);
                            }
                        });

                        $scope.experiment.restriction.technologyTypeValue = $scope.technologyTypes.find(function (techType) {
                            return techType.id == technologyType;
                        }).name;

                    } else {
                        $scope.vendors = [
                            {name: "Select study first"}
                        ];
                    }
                }

                function selectAllLabsFn(confirmSelectAllLabs) {
                    if (confirmSelectAllLabs) {
                        $scope.experiment.lab = -1;
                    } else {
                        $scope.experiment.lab = null;
                    }
                    $scope.confirmSelectAllLabs = confirmSelectAllLabs;
                }

                function showFileError(allFiles, newFiles) {
                    if(!$scope.analysisStepConfiguration.api || $scope.step < 2) {
                        return false;
                    }
                    return !$scope.analysisStepConfiguration.api.validate();
                }

                function isFilesSelected() {
                    return util.getSelectedFiles().length > 0;
                }

                function noInstrumentModels() {
                    if ($scope.analysisStepConfiguration.api) {
                        var vendorsAreLoaded = $scope.vendors != undefined;
                        var hasInstrumentModel = $scope.analysisStepConfiguration.api.getInstrumentModels().length != 0;
                        return vendorsAreLoaded && !hasInstrumentModel;
                    }
                }

                function isFormInvalid(step) {
                    // return false;
                    if (step != 1) {
                        $scope.bProteinSearchesWillBeRemoved = areProteinSearchesWillBeRemoved();
                    }
                    switch (step) {
                        case 1:

                            return $scope.validation.form && $scope.validation.form.$invalid
                                || $scope.isExperimentNameDuplicated()
                                || noInstrumentModels();
                        case 2:
                            // label assignment
                            return !$scope.analysisStepConfiguration.api.validate();
                        case 3:
                            return util.getSelectedFiles().length < 1;
                        case 4: // prepared sample to file assignment
                            return !$scope.fileToPrepAssignmentStepConfiguration.api.validate();
                        case 5: // bio sample to prepared sample assignment
                            return !$scope.sampleAssignmentStepConfiguration.api.validate();
                        case 6:
                            return !$scope.sampleToFactorsStepConfiguration.api.validate();
                    }
                }

                function isAnyFormInvalid() {
                    for (var step = 1; step <= 5; step++) {
                        if ($scope.isFormInvalid(step)) {
                            return true;
                        }
                    }
                    return false;
                }


                function confirmAndFinish() {
                    $scope.disableFinishButton = true;
                    $scope.experiment.lockMasses = $scope.lockMasses;
                    var fileToPreparedSampleNameMap = {};
                    $($scope.experiment.files).each(function (i, file) {
                        fileToPreparedSampleNameMap[file.id] = file.preparedSampleName;
                    });
                    $scope.experiment.files = util.getSelectedFiles().map(function (file) {
                        var result = {
                            id: file.id,
                            copy: file.copy,
                            fractionNumber: file.fractionNumber,
                            factorValues: $scope.experiment.factors.map(function (factor) {
                                return file.factors[factor.id];
                            })
                        };
                        result.annotations = [];
                        return result;
                    });
                    $scope.experiment.info.description = $scope.experiment.info.description ? $scope.experiment.info.description : "";
                    // specify data selected on analysis to experiment
                    var analysisSelection = $scope.analysisStepConfiguration.api.getSelected();
                    $scope.experiment.is2dLc = analysisSelection.is2dLc;
                    $scope.experiment.info.experimentType = analysisSelection.experimentType;
                    $scope.experiment.experimentLabels = analysisSelection.experimentLabels;

                    CommonLogger.log("Saving " + angular.toJson($scope.experiment));


                    var instrument = $scope.experiment.restriction.instrument;
                    if (isNumber(instrument) && instrument == -1 || !isNumber(instrument)) $scope.experiment.restriction.instrument = null;

                    var saveRequest = angular.extend({}, $scope.experiment);

                    addSamplesToFiles(saveRequest.files, saveRequest.samples, saveRequest.preparedSamples, fileToPreparedSampleNameMap);
                    delete saveRequest.preparedSamples;
                    delete saveRequest.samples;

                    saveRequest.lab = (saveRequest.lab && (saveRequest.lab > 0)) ? saveRequest.lab : null;


                    Experiments.save(saveRequest, function (data) {
                        CommonLogger.log("Experiment saved. Data = " + JSON.stringify(data));

                        //update the attachments upon the saved experiment once it has been successfully saved
                        var attachmentIds = $scope.existingAttachments.map(function (attachment) {
                            return attachment.attachmentId;
                        });
                        $scope.modalReturnUrl = createMode ? "/experiments/my" : $scope.returnUrl;

                        ExperimentAttachments.attachToExperiment({
                            experimentId: data.experimentId,
                            attachmentIds: attachmentIds
                        }, function () {
                            $(".modal").modal("hide");

                            if ($rootScope.isFeatureAvailable(LabFeatures.BILLING)) {
                                ExperimentTranslation.status({id: data.experimentId}, function (status) {
                                    if ((status.status == "NOT_STARTED" || status.status == "FAILURE") && (saveRequest) && $scope.isMs()) {
                                        var labToBill = (saveRequest.billLab) ? saveRequest.billLab : saveRequest.lab;
                                        translateExperimentConfirmation($scope, data.experimentId, labToBill);
                                    }
                                });
                            }

                        });

                        //update the attachments upon the saved experiment once it has been successfully saved
                        var annotationAttachmentIds = $scope.existingAnnotationAttachmentModel.existingAttachments.map(function (attachment) {
                            return attachment.attachmentId;
                        });
                        ExperimentAnnotationAttachment.attachToExperiment({
                            experimentId: data.experimentId,
                            annotationAttachmentId: annotationAttachmentIds[0]
                        }, function () {
                            console.log("Attachment attached " + annotationAttachmentIds[0] + " to experiment " + data.experimentId);
                        });

                        experimentByFiles.clear();
                    });

                    function addSamplesToFiles(files, samplesCopy, preparedSamplesCopy, fileToPreparedSampleNameMap) {

                        var sampleNameWithTypeToSample = composeSampleNameWithTypeToSampleMap(samplesCopy);
                        var prepSampleNameToSample = composePrepSampleNameToSampleMap(preparedSamplesCopy, sampleNameWithTypeToSample);

                        $(files).each(function (i, file) {
                            var prepSampleName = fileToPreparedSampleNameMap[file.id];
                            file.preparedSample = prepSampleNameToSample[prepSampleName];

                            delete file.factorValues; // delete unexpected fields
                            delete file.annotations; // delete unexpected fields
                        });

                        function composeSampleNameWithTypeToSampleMap(samplesCopy) {
                            var sampleNameWithTypeToSample = {};
                            $(samplesCopy).each(function (i, sample) {
                                delete sample.files; // delete unexpected fields
                                delete sample.preparedSamples; // delete unexpected fields
                                var tempFactorValues = [];
                                $.each(sample.factorValues, function (key, factorValue) {
                                    tempFactorValues.push(factorValue);
                                });
                                sample.factorValues = tempFactorValues;
                                sampleNameWithTypeToSample[composeSampleUniqueKey(sample.name, sample.type)] = sample;

                            });
                            return sampleNameWithTypeToSample;
                        }

                        function composePrepSampleNameToSampleMap(preparedSamples, sampleNameWithTypeToSample) {
                            var prepSampleNameToSample = {};
                            var preparedSamplesCopy = angular.copy(preparedSamples);
                            $(preparedSamples).each(function (i, prepSample) {
                                var samplesInArray = [];
                                $.each(prepSample.samples, function (sampleType, sampleName) {
                                    var sampleForPrepSample = sampleNameWithTypeToSample[composeSampleUniqueKey(sampleName, sampleType)];
                                    if (sampleForPrepSample) {
                                        samplesInArray.push(sampleForPrepSample);
                                    }

                                });
                                preparedSamplesCopy[i].samples = samplesInArray;
                                prepSampleNameToSample[prepSample.name] = preparedSamplesCopy[i];
                            });
                            return prepSampleNameToSample;
                        }
                        function composeSampleUniqueKey(sampleName, sampleType){
                            return sampleName + sampleType;
                        }
                    }
                }

                function selectAll(selectAll, items) {
                    angular.forEach(items, function (file) {
                        file.selected = selectAll;
                    });
                    $scope.selectedFilesLength = util.getSelectedFiles().length;
                    event.stopPropagation();
                }

                function changeSelection(file) {
                    $scope.ignoreFilesOrdering = true;
                    file.selected = !file.selected;
                    $scope.selectedFilesLength = util.getSelectedFiles().length;
                }

                function restoreFilesOrdering() {
                    $scope.ignoreFilesOrdering = false;
                }

                function isExperimentNameDuplicated() {
                    if (!$scope.experiment.info.name) return false;
                    var experiments = $.grep($scope.expShortDetails, function (expShortDetails) {
                        return (expShortDetails.name.trim().toUpperCase() === $scope.experiment.info.name.trim().toUpperCase()) && $scope.experiment.id != expShortDetails.id;
                    });
                    return experiments.length != 0;
                }

                function onDisableFilesFilter(){
                    if(!$scope.filesFilter.fromEnabled){
                        $scope.filesFilter.fromDate = "";
                    }
                    if(!$scope.filesFilter.toEnabled){
                        $scope.filesFilter.toDate = "";
                    }
                    if($scope.filesFilter.enabled &&
                        !$scope.containsEnabled && !$scope.filesFilter.fromEnabled && !$scope.filesFilter.toEnabled){
                        $scope.filesFilter.enabled = false;
                        util.refreshFilesFn()();
                    }
                }

                function disableFilesFilter(){
                    $scope.filesFilter.enabled = false;
                    $scope.filesFilter.fromEnabled = false;
                    $scope.filesFilter.toEnabled = false;
                    $scope.filesFilter.containsEnabled = false;
                }

                function applyFilesFilter(){
                    if(($scope.filesFilter.fromEnabled && $scope.filesFilter.fromDate) ||
                        ($scope.filesFilter.toEnabled && $scope.filesFilter.toDate) ||
                        ($scope.filesFilter.containsEnabled && $scope.filesFilter.contains)){
                        $scope.filesFilter.enabled = true;
                        util.refreshFilesFn(null, {itemsPerPage: ExperimentsConstants.FILES_COUNT_WITH_FILTER_APPLIED})();
                    }
                }

                function preProcessBackStep(currentStep){
                    if(currentStep == 2){
                        reloadFiles();
                    }
                }

                function areProteinSearchesWillBeRemoved() {
                    //TODO:2015-12-07:andrii.loboda: check whether is2Dlc changed and its fractions
                    if (experimentBeforeUpdate.numberOfProteinSearches == 0) {
                        return false;
                    }
                    if (!createMode) {
                        if (experimentBeforeUpdate.project != $scope.experiment.project
                            || experimentBeforeUpdate.restriction.instrument != $scope.experiment.restriction.instrument
                            || experimentBeforeUpdate.restriction.instrumentModel != $scope.experiment.restriction.instrumentModel) {
                            return true;
                        }
                        if (experimentBeforeUpdate.files.length != experiment.files.length) {
                            return true;
                        }
                        //TODO:2015-12-14:andrii.loboda: check for content, not only length check
                        if (!experimentBeforeUpdate.preparedSamples || experimentBeforeUpdate.preparedSamples.length != experiment.preparedSamples.length) {
                            return true;
                        }
                        var newSamples = experiment.samples.map(function (sample) {
                            var result = {
                                id: sample.id,
                                name: sample.name,
                                factorValues: $scope.experiment.factors.map(function (factor) {
                                    return sample.factorValues[factor.id];
                                })
                            };
                            return result;
                        });

                        if (!experimentBeforeUpdate.samples || experimentBeforeUpdate.samples.length != newSamples.length) {
                            return true;
                        }
                        var oldSamplesMap = {};
                        $(experimentBeforeUpdate.samples).each(function (i, oldSample) {
                            oldSamplesMap[oldSample.name] = oldSample;
                        });

                        var bFactorsEqual = true;
                        angular.forEach($scope.experiment.factors, function (factor, i) {
                            if (!experimentBeforeUpdate.factors[i] || experimentBeforeUpdate.factors[i].name != factor.name) {
                                bFactorsEqual = false;
                            }
                        });
                        if (!bFactorsEqual) {
                            return true;
                        }

                        var samplesEqual = true;
                        angular.forEach(newSamples, function (sample, i) {
                            if (!oldSamplesMap[sample.name] || !oldSamplesMap[sample.name].factorValues || oldSamplesMap[sample.name].factorValues.length != sample.factorValues.length) {
                                samplesEqual = false;
                                return;
                            }
                            angular.forEach(oldSamplesMap[sample.name].factorValues, function (factorValue, i) {
                                if (factorValue != sample.factorValues[i]) {
                                    samplesEqual = false;
                                }
                            })
                        });
                        if (!samplesEqual) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        };

        function updateExperimentExperimentModelForView(experiment) {
            var prepSamples = {};
            var samples = {};
            $(experiment.files).each(function (i, file) {
                if (file.preparedSample) {
                    prepSamples[file.preparedSample.name] = file.preparedSample;
                    var sampleTypeToName = {};
                    file.fractionNumber = "" + file.fractionNumber; // toString
                    $(file.preparedSample.samples).each(function (i, sample) {
                        samples[sample.name] = sample;
                        sampleTypeToName[sample.type] = sample.name;
                    });
                    file.preparedSample.samples = sampleTypeToName;
                }
            });
            experiment.preparedSamples = asArray(prepSamples);
            experiment.samples = asArray(samples);

            function asArray(map) {
                var array = [];
                $.each(map, function (key, value) {
                    array.push(value);
                });
                return array;
            }
        }
    })
    .factory("initSearchWizard", function () {
    })
    .factory("reTranslateInvalidExperimentFilesConfirmation", function($location, FilesTranslation){
        return function ($scope, experiment) {
            return function () {
                var files = [];
                angular.forEach($scope.invalidMs1FunctionItemsForDMS, function (item) {
                    var contains = $.grep(this, function(val){
                        return val.fileId == item.fileId;
                    });
                    if(contains.length <= 0){
                        this.push(item);
                    }
                }, files);

                $scope.confirmation = new (function (element) {
                    return {
                        success: function () {
                            var request = {"files": [], "lab": experiment.lab};
                            angular.forEach(files, function (item) {
                                this.files.push(item.fileId);
                            }, request);
                            console.log(request);
                            FilesTranslation.reTranslateSelected(request, function(){
                                $location.url("/experiments/my");
                            });
                        },
                        ok: function () {
                            this.success();
                        },
                        showPopup: function () {
                            $(element).css({"display": "table"});
                        },
                        hidePopup: function () {
                            $(element).hide();
                        }
                    }
                })("#re-translate-invalid-files-confirmation");
                $scope.confirmation.files = files;
                $scope.confirmation.showPopup();
            }
        }
    })
    .factory("translateExperimentConfirmation", ["ExperimentTranslation", "loadTemplate", function (ExperimentTranslation, loadTemplate) {

        return function ($scope, experimentId, chargedLab) {

            $scope.translateExperimentPopup = {};
            $scope.translateExperimentPopup.ok = function () {
                console.log("** Translate experiment: " + experimentId + ", chargedLab: " + chargedLab);
                ExperimentTranslation.translateNotTranslated({id: experimentId, chargedLab: chargedLab}, function () {
                    console.log("*** Experiment translation request was send");
                    hideModal();
                })
            };

            loadTemplate($scope, "body",
                "../pages/component/translate-experiment-dialog.html",
                function () {
                    $("#translate-experiment-confirm").modal("show");
                });

        }
    }])
    .factory("loadTemplate", function loadTemplate($http, $compile) {

        return function ($scope, element, templateUrl, onLoad) {
            $http.get(templateUrl)
                .then(function (template) {
                    $(element).append($compile(template.data)($scope));
                    onLoad();
                });
        }
    })
    .controller("newExperiment", function ($scope, $rootScope, initWizard, Security, experimentByFiles) {
        CommonLogger.setTags(["EXPERIMENTS", "NEW-EXPERIMENT-CONTROLLER"]);
        $scope.page.title = "Create Experiment";
        $scope.actionCaption = "Create";
        $scope.closeWizardActionCaption = "Cancel";

        var match = $rootScope.returnUrl.match(/\/([\d]+)\/experiments(\/([\d]+)\/files)?$/);

        var newExperiment = {
            restriction: {
                instrument: null,
                instrumentModel: null
            },
            lab: undefined,
            info: {},
            is2dLc: false,
            factors: [],
            project: match ? match[1] : undefined
        };

        if (experimentByFiles.getFiles().length) {
            newExperiment.lab = experimentByFiles.getLabId();
            newExperiment.info.specie = experimentByFiles.getSpecieId();
            newExperiment.restriction.instrument = experimentByFiles.getInstrumentId();
            newExperiment.restriction.instrumentModel = experimentByFiles.getModelId();
            newExperiment.restriction.vendor = experimentByFiles.getVendorId();
            newExperiment.restriction.technologyType = experimentByFiles.getTechnologyTypeId();
            newExperiment.restriction.instrumentType = experimentByFiles.getInstrumentTypeId();
            newExperiment.files = experimentByFiles.getFiles();
        }

        Security.get({path: ""}, function (user) {
            $scope.loggedInUser = user;
            initWizard.createMode($scope, newExperiment, $scope.loggedInUser, true, experimentByFiles.getFiles().length > 0, newExperiment.lab && $.inArray(newExperiment.lab, user.labs) < 0);
        });
    })
    .controller("experiment-details", function ($rootScope, $scope, $location, $routeParams, ExperimentDetails, Experiments, initWizard, Security) {
        CommonLogger.setTags(["EXPERIMENTS", "EXPERIMENT-DETAILS-CONTROLLER"]);
//        if ($scope.pathError) return;
        $scope.actionCaption = "Save";

        var id = $routeParams.experiment;

        ExperimentDetails.get({id: id}, function (response) {
            if (response.errorMessage) {
                var path = $location.path();
                $scope.returnUrl = $scope.defaultUrl.lastIndexOf("/files") != -1 ? $scope.defaultUrl : path.substring(0, path.lastIndexOf("/"));
                hideModal();
                return;
            }
            var experiment = response.details;
            $scope.lockMasses = experiment.lockMasses;
            Security.get({path: ""}, function (user) {
                $scope.loggedInUser = user;
                $scope.editMode = ($scope.loggedInUser.username == experiment.ownerEmail || experiment.labHead == $scope.loggedInUser.id);
                $scope.page.title = ($scope.editMode) ? "Edit Experiment" : "Experiment Details";
                $scope.closeWizardActionCaption = ($scope.editMode) ? "Cancel" : "Close";
                ($scope.editMode) ? initWizard.createMode($scope, experiment, $scope.loggedInUser, false) :
                    initWizard.viewMode($scope, experiment);
            });
        });
    })
    .controller("copyExperiment", function ($rootScope, $scope, $location, $routeParams, ExperimentDetails, Experiments, initWizard, Security) {
        if ($scope.pathError) return;
        CommonLogger.setTags(["EXPERIMENTS", "COPY-EXPERIMENT-CONTROLLER"]);
        $scope.actionCaption = "Save";

        var id = $routeParams.experiment;

        ExperimentDetails.get({id: id}, function (response) {
            if (response.errorMessage) {
                var path = $location.path();
                $scope.returnUrl = $scope.defaultUrl.lastIndexOf("/files") != -1 ? $scope.defaultUrl : path.substring(0, path.lastIndexOf("/"));
                hideModal();
                return;
            }
            var experiment = response.details;
            $scope.lockMasses = experiment.lockMasses;
            Security.get({path: ""}, function (user) {
                $scope.loggedInUser = user;
                $scope.page.title = "Copy Experiment";
                $scope.closeWizardActionCaption = "Cancel";
                var copyExperiment = $.extend(true, {}, experiment);
                copyExperiment.info.name = copyExperiment.info.name + " Copy";
                copyExperiment.id = null;
                copyExperiment.numberOfProteinSearches = 0;
                initWizard.createMode($scope, copyExperiment, $scope.loggedInUser, false, false, false, experiment.id);
            });
        });
    })
    .controller("experiments", function ($scope, $rootScope, $location, $routeParams, $route, $window, Experiments, ExperimentMoveToStorage,
                                         ExperimentDetails, removeExperimentConfirmation, experimentDownloadLink,
                                         AnonymousDownloadEmailer, experimentDisplaySearches, downloadFiles, contentRequestParameters,
                                         experimentsExpandMenu, Users, initFilesOperationsConfirmations, experimentIconDetails, downloadExperiment,
                                         Laboratories, getExperimentColumnsForAdvancedFilter, PaginationPropertiesSettingService, ExperimentColumns,
                                         changeableColumnsHelper) {

        if ($scope.pathError) return;
        CommonLogger.setTags(["EXPERIMENTS", "EXPERIMENTS-CONTROLLER"]);
        $scope.page.title = "Experiments";
        $scope.page.filterScope = $scope;
        $scope.page.showPageableFilter = true;
        var pagedRequest = contentRequestParameters.getParameters("experiments");
        $scope.page.advancedFilter = {
            composedFilter: pagedRequest.advancedFilter ? angular.copy(pagedRequest.advancedFilter) : {},
            configuration: {
                pageable: true,
                fields: getExperimentColumnsForAdvancedFilter()
            }
        };
        $scope.total = 0;
        var isTableEmpty = false;
        $scope.filter = $routeParams.filter;
        $scope.page.subtitle = $scope.$eval("filter | filterToString");
        $scope.iconDetails = experimentIconDetails;
        experimentsExpandMenu($scope);
        $scope.users = Users.query();
        $scope.labs = Laboratories.query();

        $scope.page.changeableExperimentsColumns = true;

        changeableColumnsHelper($scope, ExperimentColumns);

        var searchParams = $location.search();
        if (searchParams.downloadExperiment) {
            setTimeout(function () {
                downloadExperiment($scope, {id: searchParams.downloadExperiment}, true);
            });
        }

        pagedRequest.labId = 0;
        if ($routeParams.labId) {
            pagedRequest.labId = $routeParams.labId * 1;
        }
        Experiments.get(pagedRequest, function (experimentsCollection) {
            $scope.experiments = $.map((experimentsCollection.items || experimentsCollection), function (item) {
                return item;
            });
            PaginationPropertiesSettingService.setPaginationProperties($scope, experimentsCollection);
            isTableEmpty = $scope.total == 0;
        });

        $scope.isTableEmpty = function () {
            return isTableEmpty;
        };
        $scope.getEmptyTableMessage = function () {
            return "There are no experiments";
        };

        $scope.showPermanentLink = experimentDownloadLink($scope, AnonymousDownloadEmailer, downloadFiles);

        $scope.showRequestDownloadPopup = function (experiment) {
            $scope.requestDownloadPopup = new Confirmation("#request-experiment-download-popup", experiment, {
                success: function () {
                    ExperimentMoveToStorage.query({id: experiment.id, actor: $scope.getUserId()});
                }
            });
            $scope.requestDownloadPopup.showPopup();
        };

        initFilesOperationsConfirmations($scope);

        $scope.downloadExperiment = function (experiment) {
            downloadExperiment($scope, experiment);
        };
        $scope.showAnalysisWizard = function (experiment) {
            $rootScope.dialogReturnUrl = $location.url();
            $location.path($location.path() + "/" + experiment.id + "/searches/new");
        };
        $scope.showRuns = experimentDisplaySearches();
        $scope.displayConfirmation = removeExperimentConfirmation($scope);
        $scope.copyExperiment = function (experiment) {
            $location.path($location.path() + "/" + experiment.id + "/copy");
        };

    })
    .controller("experiment-upload-fasta-db", function ($scope, $rootScope, $routeParams, Security, ExperimentDetails, startNewUpload, ProteinDB, ExperimentSpecies) {
        CommonLogger.setTags(["EXPERIMENTS", "EXPERIMENT-UPLOAD-FASTA-DB-CONTROLLER"]);
        $scope.page.title = "Upload Protein Database";
        $scope.closeWizardActionCaption = "Cancel";
        $scope.db = {};
        $scope.step = 1;
        ExperimentSpecies.query(function (species) {
            $scope.species = species;
        });
        $scope.dbs = ProteinDB.items(function (data) {
            $scope.dbTypesMap = {};
        });

        $scope.isDBNameDuplicated = function () {
            var dbs = $.grep($scope.dbs, function (db) {
                return (db.name == $scope.db.name);
            });
            return dbs.length != 0;
        };
        var getAllowedExtensionsFn = function () {
            return [".fasta"];
        };
        $scope.getAllowedExtensionsAsString = function () {
            return getAllowedExtensionsFn().join(", ");
        };
        $scope.isFormInvalid = function () {
            if (!$scope.db.name || !$scope.db.type || $scope.uploadingAttachments.length == 0 || $scope.isDBNameDuplicated()) {
                return true;
            }
            return false;
        };

        $scope.uploadFiles = function () {
            $scope.step = 2;
            AttachmentsHelper.uploadFiles($scope, ProteinDB);
        };

        $scope.existingAttachments = [];
        AttachmentsHelper.commonSetup($scope, null, "#upload-fasta-db-dialog", ProteinDB,
            startNewUpload, null, true, getAllowedExtensionsFn, {
                attachmentType: "Protein Fasta DB",
                postponedUpload: true,
                fileChooserId: "#fastaFileChooser",
                isSingleFileUpload: true,
                getDataToSendFn: function () {
                    return {
                        bPublic: $scope.db.bPublic,
                        bReversed: $scope.db.bReversed,
                        dbType: $scope.db.type,
                        name: $scope.db.name
                    }
                },
                uploadComplete: function (proteinDnId, contentUrl) {
                    if ($scope.existingAttachments.length == 1) {
                        ProteinDB.updateContent({proteinDbId: proteinDnId, contentUrl: contentUrl}, function (data) {
                            $scope.step = 3;
                            CommonLogger.log("Protein database saved. Data = " + JSON.stringify(data));
                            setTimeout(function () {
                                $(".modal").modal("hide");
                            }, 0);
                        });
                    }
                },
                allowAllFileTypes: false
            });

    })
    .controller("protein-search-databases", function ($scope, $location, $routeParams, $route, ProteinDB) {
        var filter = $routeParams.show;
        $scope.page.title = "Search Databases";
        $scope.page.showFilter = true;
        $scope.page.advancedFilter = {
            composedFilter: {},
            configuration: {
                fields: [
                    {prop: "id", title: "ID", type: "number"},
                    {prop: "name", title: "Name", type: "string"},
                    {prop: "typeName", title: "Species Type", type: "string"},
                    {prop: "bPublic", title: "Shared", type: "boolean"},
                    {prop: "bReversed", title: "Reversed", type: "boolean"},
                    {prop: "uploadDate", title: "Upload Date", type: "date"}
                ]
            }
        };

        $scope.page.filter = "";
        var isTableEmpty = false;
        switch(filter){
            case "all": ProteinDB.itemsAccessibleByUser(processDatabases); break;
            case "my": ProteinDB.myItems(processDatabases); break;
            case "public": ProteinDB.publicItems(processDatabases);
        }


        $scope.formatAccessValue = function (isPublic) {
            return isPublic ? "Yes" : "No";
        };
        $scope.isTableEmpty = function () {
            return isTableEmpty;
        };
        $scope.getEmptyTableMessage = function () {
            return "There are no search databases";
        };

        $scope.displayConfirmation = function (database) {
            $scope.confirmation = new Confirmation("#remove-database-confirmation", database,
                {
                    success: function () {
                        ProteinDB.remove({id: database.id}, function () {
                            $route.reload();
                        })
                    },
                    getName: function () {
                        return database.name;
                    }
                });
            $scope.confirmation.showPopup();
        };

        $scope.updateSharing = function(database){
            ProteinDB.share({id: database.id, bPublic: !database.bPublic}, function(){
                $route.reload();
            });
        };

        $scope.duplicate = function(database){
            $scope.$broadcast(ExperimentsConstants.SHOW_DUPLICATE_SEARCH_DATABASE_DIALOG ,database.id);
        };

        function processDatabases(searchDatabases) {
            $scope.searchDatabases = searchDatabases;
            isTableEmpty = $scope.searchDatabases.length == 0;
        }

    })
    .directive("duplicateProteinSearchDatabase", function(){
        return {
            restrict: "E",
            templateUrl: "../pages/experiment/protein-search/databases/duplicate.html",
            scope: {},
            link: function(scope, elem){
                scope.showDialog = function(){
                    elem.find(".modal").modal("show");
                };
            },
            controller: function($scope, $location, ProteinDB){
                $scope.showLoading = true;
                $scope.$on(ExperimentsConstants.SHOW_DUPLICATE_SEARCH_DATABASE_DIALOG, function(event, databaseId){
                    $scope.showDialog();
                    duplicate(databaseId);
                });

                function duplicate(databaseId){
                    ProteinDB.duplicate({id: databaseId}, function(response){
                        $scope.showLoading = false;
                        if(!response.errorMessage){
                            $location.url("/protein-search/databases/my/" + response.id);
                        }
                        $scope.errorMessage = response.errorMessage;
                    });
                }
            }
        }
    })
    .controller("protein-search-database-details", function ($scope, $location, $routeParams, $route, ProteinDB, ExperimentSpecies) {
        $scope.canEditType = false;
        $scope.dbs = [];
        $scope.database = {};

        ProteinDB.get({id: $routeParams.id}, function (database) {
            $scope.database = database;
            $scope.canEditType = database.accessible;
        });
        ProteinDB.items(function (dbs) {
            $scope.dbs = dbs;
        });
        ExperimentSpecies.query(function (species) {
            $scope.species = species;
        });

        $scope.isDBNameDuplicated = function () {
            var dbs = $.grep($scope.dbs, function (db) {
                return db.name == $scope.database.name
                    && db.id != $scope.database.id;
            });
            return dbs.length != 0;
        };

        $scope.isFormInvalid = function () {
            return !$scope.database.name || !$scope.database.typeId || $scope.isDBNameDuplicated();
        };

        $scope.save = function () {
            if ($scope.isFormInvalid()) return;

            var searchDatabase = {
                databaseId: $routeParams.id,
                name: $scope.database.name,
                typeId: $scope.database.typeId
            };
            ProteinDB.updateDetails(searchDatabase, function () {
                console.log("Search Database " + searchDatabase.id + " updated.");
                setTimeout(function () {
                    $(".modal").modal("hide");
                }, 0);
            });
        };
    })
    .controller("project-experiments", function ($scope, $rootScope, $location, $routeParams,
                                                 $window, ExperimentsByProject, ProjectDetails, ExperimentDetails,
                                                 experimentDownloadLink, AnonymousDownloadEmailer,
                                                 experimentDisplaySearches, contentRequestParameters, experimentsExpandMenu, experimentIconDetails,
                                                 downloadFiles, ExperimentMoveToStorage, removeExperimentConfirmation, Users, initFilesOperationsConfirmations, Laboratories, downloadExperiment, getExperimentColumnsForAdvancedFilter,
                                                 PaginationPropertiesSettingService) {
        if ($scope.pathError) return;
        CommonLogger.setTags(["EXPERIMENTS", "PROJECT-EXPERIMENTS-CONTROLLER"]);
        ProjectDetails.get({id: $routeParams.project}, function (project) {
            $scope.page.title = $routeParams.project + ": " + project.details.name;
            $scope.page.res = {
                type: "PROJECT",
                id: $routeParams.project,
                path: $location.$$url.substring(0, $location.$$url.lastIndexOf("/" + $routeParams.project + "/experiments")),
                returnUrl: $location.$$url,
                canEdit: ($scope.getLoggedUserName() == project.details.ownerEmail || $scope.getUserId() == project.details.labHead)
            };
        });
        experimentsExpandMenu($scope);
        initFilesOperationsConfirmations($scope);

        $scope.page.filterScope = $scope;
        $scope.page.showPageableFilter = true;
        var isTableEmpty = false;
        $scope.total = 0;
        $scope.iconDetails = experimentIconDetails;
        $rootScope.returnUrl = $location.$$url.substring(0, $location.$$url.lastIndexOf("/" + $routeParams.project + "/experiments"));
        $scope.users = Users.query();
        $scope.labs = Laboratories.query();

        var pagedRequest = contentRequestParameters.getParameters("experiments");
        $scope.page.advancedFilter = {
            composedFilter: pagedRequest.advancedFilter ? angular.copy(pagedRequest.advancedFilter) : {},
            configuration: {
                pageable: true,
                fields: getExperimentColumnsForAdvancedFilter()
            }
        };
        ExperimentsByProject.getPage({
            id: $routeParams.project,
            page: pagedRequest.page,
            items: pagedRequest.items,
            sortingField: pagedRequest.sortingField,
            asc: pagedRequest.asc,
            filterQuery: pagedRequest.filterQuery,
            advancedFilter: pagedRequest.advancedFilter
        }, function (experimentsCollection) {

            $scope.experiments = $.map((experimentsCollection.items || experimentsCollection), function (item) {
                return item;
            });

            PaginationPropertiesSettingService.setPaginationProperties($scope, experimentsCollection);
            isTableEmpty = $scope.total == 0;

        });

        $scope.filter = $routeParams.filter;

        $scope.isTableEmpty = function () {
            return isTableEmpty;
        };

        $scope.getEmptyTableMessage = function () {
            return "There are no experiments";
        };

        $scope.showPermanentLink = experimentDownloadLink($scope, AnonymousDownloadEmailer, downloadFiles);
        $scope.showRequestDownloadPopup = function (experiment) {
            $scope.requestDownloadPopup = new Confirmation("#request-experiment-download-popup", experiment, {
                success: function () {
                    ExperimentMoveToStorage.query({id: experiment.id, actor: $scope.getUserId()});
                }
            });
            $scope.requestDownloadPopup.showPopup();
        };

        $scope.downloadExperiment = function (experiment) {
            downloadExperiment($scope, experiment);
        };

        $scope.showAnalysisWizard = function (experiment) {
            $rootScope.dialogReturnUrl = $location.url();
            $location.path($location.path() + "/" + experiment.id + "/searches/new");
        };

        $scope.displayConfirmation = removeExperimentConfirmation($scope);
        $scope.copyExperiment = function (experiment) {
            $location.path($location.path() + "/" + experiment.id + "/copy");
        };

        $scope.showRuns = experimentDisplaySearches();

    })
    .run(function (registerBreadcrumbHandler, ProjectDetails, ExperimentDetails) {
        var getFilterExperimentsPath = function (path) {
            return path.replace(/\/([\d]+)$/, "");
        };
        var getFilterExperimentsPathFromProcessingRunsPath = function (path) {
            return getFilterExperimentsPath(path.replace(/\/searches/, ""));
        };
        var getExperimentsInProjectPathFromFilesPath = function (path) {
            return path.replace(/\/([\d]+)\/files$/, "");
        };

        registerBreadcrumbHandler(function (path) {
            var matchForFilesInExperimentInProject = path.match(/\/projects\/(all|my|shared|public)\/([\d]+)\/experiments\/([\d]+)\/files$/);
            var matchForProcessingRunInExperimentInProject = path.match(/\/projects\/(all|my|shared|public)\/([\d]+)\/experiments\/([\d]+)\/searches$/);
            var breadcrumb;
            if (matchForFilesInExperimentInProject) {
                breadcrumb = {label: "Loading...", url: "#" + getExperimentsInProjectPathFromFilesPath(path)};
                ProjectDetails.get({id: matchForFilesInExperimentInProject[2]}, function (project) {
                    breadcrumb.label = matchForFilesInExperimentInProject[2] + ": " + project.details.name;
                });
            } else if (matchForProcessingRunInExperimentInProject) {
                breadcrumb = {label: "Loading...", url: "#" + getFilterExperimentsPathFromProcessingRunsPath(path)};
                ProjectDetails.get({id: matchForProcessingRunInExperimentInProject[2]}, function (project) {
                    breadcrumb.label = matchForProcessingRunInExperimentInProject[2] + ": " + project.details.name;
                });
            } else {
                var matchForExperiment = path.match(/\/experiments\/(all|my|shared|public)\/([\d]+)$/);
                var matchForProcessingRuns = path.match(/\/experiments\/(all|my|shared|public)\/([\d]+)\/searches$/);
                if (matchForExperiment) {
                    var filter = matchForExperiment[1];
                    if (path.indexOf("shared") != -1) {
                        breadcrumb = {
                            label: filter[0].toUpperCase() + filter.substring(1) + " with Me Experiments",
                            url: "#" + getFilterExperimentsPath(path)
                        };
                    } else {
                        breadcrumb = {
                            label: filter[0].toUpperCase() + filter.substring(1) + " Experiments",
                            url: "#" + getFilterExperimentsPath(path)
                        };
                    }
                } else if (matchForProcessingRuns) {
                    /*var filter = matchForProcessingRuns[1];
                     if (path.indexOf("shared") != -1) {
                     breadcrumb = {label: filter[0].toUpperCase() + filter.substring(1) + " with Me Experiments", url: "#" + getFilterExperimentsPathFromProcessingRunsPath(path)};
                     }else{
                     breadcrumb = {label: filter[0].toUpperCase() + filter.substring(1) + " Experiments", url: "#" + getFilterExperimentsPathFromProcessingRunsPath(path)};
                     }*/
                } else {
                    breadcrumb = null;
                }
            }
            return breadcrumb;
        });
    })
    .filter("selected", function () {
        return function (files) {
            var result = [];
            angular.forEach(files, function (file) {
                if (file.selected) {
                    result.push(file);
                }
            });
            return result;
        }
    })
    .filter("labels", function () {
        return function (labels) {
            return labels;
        }
    })
    .filter("factorName", function () {
        return function (factor) {
            var result = factor.name;
            if (factor.isNumeric == "true" && factor.units && factor.units.length > 0) {
                result += " (" + factor.units + ")";
            }
            return result;
        }
    })
    .directive("experimentDetails", detailsLink({
        "title": "Show Experiment Details",
        "dataTarget": "#experimentDetails"
    }))
    .directive("experimentDetailsButton", detailsDirective({
        "title": "Show Experiment Details",
        "dataTarget": "#experimentDetails"
    }))
    .directive("searchDatabaseDetails", detailsLink({
        "title": "Show Search Database Details",
        "dataTarget": "#searchDatabaseDetails"
    }))
    .directive("searchDatabaseName", linkedName({"sub": ""}))
    .directive("experimentName", linkedName({"sub": "files?page=1&items=25&sortingField=uploadDate&asc=false"}))
    .directive("inputKeyPress", function () {
        return {
            restrict: "A",
            link: function (scope, elem, attr, ctrl) {
                elem.bind("keydown", function ($event) {
                    scope.$apply(function (s) {
                        s.$eval(attr.inputKeyPress);
                    });
                });
            }
        }
    })
    .factory("removeExperimentConfirmation", function ($route, Experiments) {
        return function ($scope) {
            return function (experiment) {
                $scope.confirmation = new Confirmation("#remove-experiment-confirmation", experiment,
                    {
                        success: function () {
                            Experiments.delete({
                                experiment: experiment.id,
                                removePermanently: $scope.confirmation.removePermanently
                            }, function () {
                                $route.reload();
                            })
                        },
                        getName: function () {
                            return experiment.name;
                        }
                    }
                );
                $scope.confirmation.removePermanently = true;
                $scope.confirmation.showPopup();
            }
        }
    })
    .factory("initFilesOperationsConfirmations", ["experimentFilesOperations", "$route", "ExperimentFiles", "$rootScope", "BillingFeatures",
        function (experimentFilesOperations, $route, ExperimentFiles, $rootScope, BillingFeatures) {
            return function ($scope) {
                
                $scope.showArchiveExperimentConfirmation = showArchiveExperimentConfirmation;
                $scope.showUnarchiveExperimentConfirmation = showUnarchiveExperimentConfirmation;
                $scope.showRemoveTranslatedDataConfirmation = showRemoveTranslatedDataConfirmation;
                $scope.canArchiveFiles = canArchiveFiles;
                $scope.canUnarchiveFiles = canUnarchiveFiles;
                $scope.archivingAvailable = archivingAvailable;
                
                
                function showArchiveExperimentConfirmation(experiment) {

                    ExperimentFiles.usedInOtherExperiments({id: experiment.id}, function (files) {
                        experiment.hasFilesInAnotherExperiments = false;
                        $scope.archivePopup = new Confirmation("#archive-experiment", experiment, {
                            success: function (experiment) {
                                experimentFilesOperations.archive(experiment, $route.reload);
                            }
                        });
                        $scope.archivePopup.showPopup();

                    });
                }
                
                function showUnarchiveExperimentConfirmation(experiment) {
                    $scope.unarchivePopup = new Confirmation("#unarchive-experiment", experiment, {
                        success: function (experiment) {
                            experimentFilesOperations.unarchive(experiment, $route.reload);
                        }
                    });
                    $scope.unarchivePopup.showPopup();
                }
                
                function showRemoveTranslatedDataConfirmation(experiment) {
                    $scope.removeTranslatedDataConfirmation = new Confirmation("#remove-experiment-files-translation-data", experiment, {
                        success: function (experiment) {
                            experimentFilesOperations.removeTranslationData(experiment, function () {
                                    setTimeout(function () {
                                        $route.reload();
                                    }, 500);
                                }
                            );
                        }
                    });
                    $scope.removeTranslatedDataConfirmation.showPopup();
                }
                
                function canArchiveFiles(experiment) {
                    var expLabId = getExperimentLab(experiment);
                    return (!expLabId || $rootScope.isUserLab(expLabId))
                        && experiment.canArchive
                        && experiment.files > 0
                        && (experiment.isOwner || $rootScope.loggedInUser.id == experiment.lab.labHead);
                }
                
                function canUnarchiveFiles(experiment) {
                    var expLabId = getExperimentLab(experiment);
                    return (!expLabId || $rootScope.isUserLab(expLabId))
                        && experiment.canUnarchive
                        && experiment.files > 0
                        && (experiment.isOwner || $rootScope.loggedInUser.id == experiment.lab.labHead)
                        && ($rootScope.isBillingFeatureAvailable(BillingFeatures.ARCHIVE_STORAGE, expLabId));
                }
                
                function archivingAvailable(experiment) {
                    var labId = getExperimentLab(experiment);
                    return $rootScope.isUserLab(labId)
                        && $rootScope.isBillingFeatureAvailable(BillingFeatures.ARCHIVE_STORAGE, labId);
                }

                function getExperimentLab(experiment) {
                    return experiment.lab ?
                        experiment.lab.id :
                        (experiment.billLab ? experiment.billLab : null);
                }

            }
        }])
    .factory("experimentDisplaySearches", function ($route, $rootScope, $location) {
        return function () {
            return function (experiment) {
                $rootScope.dialogReturnUrl = $location.url();
                $location.path($location.path() + "/" + experiment.id + "/searches");
            };
        }
    })
    .factory("experimentDownloadLink", function (Security, Collaborators) {
        return function ($scope, AnonymousDownloadEmailer, downloadFiles) {
            return function (experiment) {
                if (isPublic(experiment)) {
                    $scope.permanentLinkPopup = new ExperimentDownloadLinkPopup(experiment, "#public-experiment-download-permalink",
                        AnonymousDownloadEmailer, downloadFiles, Security);
                } else {
                    $scope.users = Collaborators.query({experimentId: experiment.id});
                    $scope.permanentLinkPopup = new ExperimentDownloadLinkPopup(experiment, "#experiment-download-permalink",
                        AnonymousDownloadEmailer, downloadFiles, Security);
                }
                $scope.permanentLinkPopup.showPopup();
                function isPublic(experiment) {
                    return experiment.files > 0 && experiment.downloadLink != null && experiment.accessLevel == "PUBLIC";
                }
            }
        }
    })
    .factory("experimentFilesOperations", ["ExperimentFilesArchiving", "ExperimentTranslation",
        function (ExperimentFilesArchiving, ExperimentTranslation) {

            var fileOperations = {};

            fileOperations.archive = function (experiment, success) {
                ExperimentFilesArchiving.archive({id: experiment.id}, function () {
                    CommonLogger.log("*** Archive experiment request was send");
                    if (success) {
                        success()
                    }
                })
            };

            fileOperations.unarchive = function (experiment, success) {
                ExperimentFilesArchiving.unarchive({id: experiment.id}, function () {
                    CommonLogger.log("*** Unarchive experiment request was send");
                    if (success) {
                        success()
                    }
                })
            };

            fileOperations.translate = function (experiment, chargedLab, success) {
                ExperimentTranslation.translate({id: experiment.id, chargedLab: chargedLab}, function () {
                    CommonLogger.log("*** Experiment translation request was send");
                    if (success) {
                        success()
                    }
                })
            };

            fileOperations.removeTranslationData = function (experiment, success) {
                ExperimentTranslation.deleteTranslationData({id: experiment.id}, function () {
                    console.log("*** Experiment files translation data was removed");
                    if (success) success();
                })
            };

            return fileOperations;
        }])
    .directive("sharinglinkSelector", userOrGroupSelection({
        "isEmailNotificationsAvailable": false,
        "groupSelectionAvailable": false,
        "emptyTableMessage": "There are no members",
        "addActionText": "Invite people personally",
        "addPlaceHolderText": "Enter person's email",
        "showAllowWrite": false
    }))
    .factory("reversedSorting", ["$filter", function ($filter) {
        var reversed = false;
        return function ($scope) {
            $scope.reverseSorting = false;
            return function (reverseSorting) {
                if (reverseSorting != reversed) {
                    reversed = reverseSorting;
                    return $filter("selected")($scope.files.reverse());
                }
                return $filter("selected")($scope.files);
            };
        }
    }])
    .directive("doubleValueValidation", ["$parse", "$timeout", function ($parse, $timeout) {
        return {
            restrict: "A",
            require: "?ngModel",
            link: function (scope, element, attrs, ngModel) {
                if (!ngModel) return;

                var jElem = $(element);
                var dvvGetter = $parse(attrs.doubleValueValidation);
                var applyValidation = dvvGetter(scope);
                var cbnGetter = $parse(attrs.canBeNegative);
                var canBeNegative = cbnGetter(scope);
                var delayValidation = $parse(attrs.delayValidation)(scope);

                scope.$watch(
                    function () {
                        return jElem.text();
                    },
                    function (newVal) {
                        validationFn(newVal);
                    }
                );

                ngModel.$parsers.unshift(validationFn);

                function validationFn(value) {
                    var replacedValue = value;
                    if (applyValidation == "true" && value.length != 0) {
                        replacedValue = canBeNegative ? /[-+]?[0-9]*\.?[0-9]*/.exec(value)[0] : /[+]?[0-9]*\.?[0-9]*/.exec(value)[0];
                    }
                    if (replacedValue != value) {
                        // in case when the element is a table cell and a user pastes multiple values
                        // let the table process the values
                        if(delayValidation) {
                            $timeout(function() {
                                ngModel.$setViewValue(replacedValue);
                                ngModel.$render();
                            }, 300);
                            return value;
                        }
                        ngModel.$setViewValue(replacedValue);
                        ngModel.$render();
                    }
                    return replacedValue;
                }
            }
        }
    }])
    .directive("posNumberValidation", function () {
        return {
            restrict: "A",
            require: "?ngModel",
            link: function (scope, element, attrs, ngModel) {
                if (!ngModel) return;

                ngModel.$parsers.unshift(function (value) {
                    var replacedValue = value;
                    if (value && value.length != 0) {
                        replacedValue = /[+]?[0-9]*\.?[0-9]*/.exec(value)[0];
                    }
                    if (replacedValue != value) {
                        ngModel.$setViewValue(replacedValue);
                        ngModel.$render();
                    }
                    return replacedValue;
                });
            }
        }
    }).directive("posIntNumberValidation", function () {
        return {
            restrict: "A",
            require: "?ngModel",
            link: function (scope, element, attrs, ngModel) {
                if (!ngModel) return;

                ngModel.$parsers.unshift(function (value) {
                    var replacedValue = value;
                    if (value && value.length != 0) {
                        var res = /[+]?[1-9]+[0-9]*/.exec(value);
                        if (res && res[0]) {
                            replacedValue = res[0];
                        }
                    }
                    if (replacedValue != value) {
                        ngModel.$setViewValue(replacedValue);
                        ngModel.$render();
                    }
                    return replacedValue;
                });
            }
        }
    })
    .directive("pasteInTableValidation", function () {
        return {
            restrict: "A",
            require: "ngModel",
            link: function (scope, element, attrs, ngModel) {
                element.bind("paste", function ($event) {
                    //for Chrome
                    if ($event.originalEvent.clipboardData &&
                        $event.originalEvent.clipboardData.getData) {
                        element.text($event.originalEvent.clipboardData.getData("text/plain"));
                        $event.preventDefault();
                    } else { //other browsers
                        setTimeout(function () {
                            scope.$apply(function () {
                                var text = element.html().replace(/<br>/g, "\n").replace(/(&nbsp;[ ]?)+/g, "\t");
                                ngModel.$setViewValue(text);
                            });
                        }, 0);
                    }
                });
            }
        }
    })
    .directive("checkUiSelection", function () {
        return {
            restrict: "A",
            require: "?ngModel",
            link: function (scope, elm, attrs, ngModel) {
                if (!ngModel)  return;
                var watchModel = attrs.ngModel;
                scope.$watch(watchModel, function (newVal, oldVal) {
                    if (newVal === undefined) {
                        setTimeout(function () {
                            ngModel.$setViewValue(oldVal);
                            scope.$apply(function () {
                                elm.select2("val", ngModel.$viewValue);
                            });
                        }, 0);
                    }
                })
            }
        }
    })
    .factory("formatExperimentProject", function () {
        return function (item) {
            var text = item.text;
            var title = item.element[0]["title"];
            return "<span class='select2-results' title='" + title + "'>" + text + "</span>";
        }
    })
    .factory("experimentsExpandMenu", function (ExperimentDetails, ExperimentAttachments) {

        return initExpandMenu(function (experiment, $scope) {
            ExperimentDetails.get({id: experiment.id, path: "short"}, function (response) {
                experiment.details = response;
                var attachments = experiment.details.attachments;
                var sampleNameToConditionMap = {};
                $.each(experiment.details.files, function (i, file) {
                    $(file.samples).each(function (i, sample) {
                        sampleNameToConditionMap[sample.name] = sample.condition;
                    });
                });
                var experimentSamplesList = [];
                $.each(sampleNameToConditionMap, function (sampleName, sampleCondition) {
                    experimentSamplesList.push({name: sampleName, condition: sampleCondition});
                });
                experiment.details.experimentSamplesList = experimentSamplesList;


                experiment.attachments = $.map(attachments, function (attachment) {
                    var type = AttachmentsHelper.attachmentTypeFromName(attachment.name);
                    CommonLogger.log(type);
                    var a = new AttachmentsHelper.Attachment(attachment.name, attachment.uploadDate, attachment.sizeInBytes, type, null, null);
                    a.attachmentId = attachment.id;
                    return a;
                });
            });

            $scope.downloadAttachment = function (attachmentId) {
                $.fileDownload("../attachments/experiment/download/" + attachmentId, {});
            };
        });
    })
    .factory("downloadExperiment", function ($rootScope, Laboratories, downloadFiles, ExperimentMoveToStorage, LabFeatures) {
        return function ($scope, experiment, byLink) {

            if (byLink) {

                setupSharedExperimentDownloadPopup(experiment)

            } else if (experiment.downloadAvailable) {

                if ($rootScope.isFeatureAvailable(LabFeatures.BILLING)) {

                    var lab = experiment.billLab;
                    var fileInUserLab = $.grep($scope.labs, function (item) {
                            return item.id == lab;
                        }).length > 0;

                    if (fileInUserLab || experiment.accessLevel == "PUBLIC") {
                        downloadFiles([], experiment.id).download();
                    } else if ($scope.labs.length == 1) {
                        downloadFiles([], experiment.id, $scope.labs[0].id).download();
                    } else {
                        setupSharedExperimentDownloadPopup(experiment);
                    }

                } else {

                    downloadFiles([], experiment.id).download();

                }

            } else {

                showExperimentDownloadConfirm("Preparing job for downloading experiment " + experiment.name + " was started." +
                    "You will receive notification, when all files will be ready.", function () {
                    ExperimentMoveToStorage.query({id: experiment.id, actor: $scope.getUserId()});
                });

            }

            function setupSharedExperimentDownloadPopup(experiment) {
                $scope.downloadSharedExperimentPopup = new Confirmation("#downloadSharedExperimentPopup", experiment, {
                    success: function (item) {
                        downloadFiles([], experiment.id, $scope.downloadSharedExperimentPopup.selectedLab).download();
                    }
                });
                $scope.downloadSharedExperimentPopup.selectedLab = undefined;
                $scope.downloadSharedExperimentPopup.showPopup();
            }
        }
    })
    .controller("experimentColumnsEditor", function ($scope, ExperimentColumns, columnsEditor) {
        columnsEditor($scope, ExperimentColumns);
    })
    .controller("exposeFormToParentScope", function ($scope) {
        $scope.$watch("form", function () {
            if (!$scope.validation) {
                $scope.validation = {};
            }
            $scope.validation.form = $scope.form;
        });
    })
    .directive("analysisBounds", function () {
        return {
            templateUrl: "../pages/component/analysis-bounds.html",
            scope: {
                label: "@",
                model: "=",
                checkboxLabel: "@",
                disabled: "=",
                min: "@",
                max: "@",
                inputId: "@"
            },
            controller: function ($scope, $element, $attrs) {

                $scope.check = !$scope.model;

                $scope.$watch("check", function (check) {
                    if (check) {
                        $scope.model = "";
                    }
                });
            }
        };
    })
    .directive("annotationAttachmentsUpload", function () {
        return {
            templateUrl: "../pages/component/annotation-attachments-upload.html",
            restrict: "E",
            scope: {
                experiment: "=",
                existingAttachments: "=",
                removedAttachments: "="
            },
            controller: function ($scope, $element, $attrs, $timeout, startNewUpload, ExperimentAnnotationAttachment) {
                $scope.uploadingAttachments = [];
                $scope.attachmentCouldDownload = true;
                $scope.getAllowedExtensionsForAnnotationUploadAsString = getAllowedExtensionsForAnnotationUploadAsString;
                activate();

                function activate() {
                    // we need timeout to initialize experiment attachments in AttachmentsHelper.commonSetup
                    // after that "experiment" property will appear in $scope
                    $timeout(setupAttachments, 500);

                    function setupAttachments() {
                        //Upload CSV Annotation                     
                        AttachmentsHelper.commonSetup($scope, $scope.experiment.id, "#annotation-attachments", ExperimentAnnotationAttachment,
                            startNewUpload, "../annotations/experiment/download/", true, getAllowedExtensionsForAnnotationUploadFn, {
                                attachmentType: "Annotation Attachment",
                                displayDragAndDropAreaOnlyIfAreaPresented: true,
                                fileChooserId: "#annotationFileChooser",
                                isSingleFileUpload: true,
                                notRemoveExisted: true,
                                scope: $scope,
                                uploadComplete: function () {
                                    if ($scope.existingAttachments.length == 1) {
                                        console.log("Annotation attachment upload complete" + $scope.existingAttachments);
                                    }
                                }
                            });
                    }
                }
                function getAllowedExtensionsForAnnotationUploadFn() {
                    return [".csv"];
                }
                function getAllowedExtensionsForAnnotationUploadAsString() {
                    return getAllowedExtensionsForAnnotationUploadFn().join(", ");
                }
            }
        };
    })
    .directive("infiniteScroll", [
        "$rootScope", function($rootScope) {
            return {
                link: function(scope, element, attrs) {
                    var scrollEnabled = true;
                    var scrollDelta = 0;
                    if (attrs.infiniteScrollDisabled != null) {
                        scope.$watch(attrs.infiniteScrollDisabled, function(value) {
                            scrollEnabled = !value;
                        });
                    }
                    if (attrs.infiniteScrollDelta != null) {
                        scrollDelta = parseInt(attrs.infiniteScrollDelta) || 0;
                    }
                    var handler = function() {
                        if(element.scrollTop() + element.height() >= element[0].scrollHeight - scrollDelta){
                            if(scrollEnabled && attrs.infiniteScroll){
                                if ($rootScope.$$phase) {
                                    return scope.$eval(attrs.infiniteScroll);
                                } else {
                                    return scope.$apply(attrs.infiniteScroll);
                                }
                            }
                        }
                    };
                    element.on("scroll", handler);
                }
            };
        }
    ])
    .factory("getExperimentColumnsForAdvancedFilter", function () {
        return function () {
            return [
                {prop: "id", title: "ID", type: "number"},
                {prop: "name", title: "Name", type: "string"},
                {prop: "owner", title: "Owner", type: "string"},
                {prop: "laboratory", title: "Program", type: "string"},
                {prop: "project", title: "Project", type: "string"},
                {prop: "filesCount", title: "Files Count", type: "number"},
                {prop: "modified", title: "Modified", type: "date"},
                {prop: "description", title: "Description", type: "string"}
            ];
        }
    })
    .factory("getProcessingRunColumnsForAdvancedFilter", function () {
        return function () {
            return [
                {prop: "id", title: "ID", type: "number"},
                {prop: "name", title: "Name", type: "string"},
                {prop: "experiment.id", title: "Study ID", type: "number"},
                {prop: "experiment.name", title: "Study name", type: "string"},
                {prop: "owner", title: "Owner", type: "string"},
                {prop: "experiment.lab", title: "Program", type: "string"},
                {prop: "status", title: "Status", type: "string"},
                {prop: "lastExecuted", title: "Last Executed", type: "date"}
            ];
        }
    })
;

var ExperimentsConstants = {
    FILES_ITEMS_PER_PAGE : 30,
    SHOW_DUPLICATE_SEARCH_DATABASE_DIALOG: "SHOW_DUPLICATE_SEARCH_DATABASE_DIALOG",
    FILES_COUNT_WITH_FILTER_APPLIED: 1000
};

var ExperimentViewer = function (containerSelector, chartsUrl, translationDate, errors, experimentId,
                                 isOwner, ExperimentTranslation, wnd, archived, isLabHead, countOfOwnedFilesTranslationData,
                                 availableLabs, selectedLab, billLab, status, usedInSearches) {
    this.containerSelector = containerSelector;
    this.translationDate = translationDate;
    this.archived = archived;
    this.errors = errors;
    this.chartsUrl = chartsUrl;
    this.wnd = wnd;
    this.experimentId = experimentId;
    this.ExperimentTranslation = ExperimentTranslation;
    this.availableLabs = availableLabs;
    this.selectedLab = selectedLab;
    this.isOwner = isOwner;
    this.isLabHead = isLabHead;
    this.billLab = billLab;
    this.status = status;
    this.usedInSearches = usedInSearches;

    var _this = this;

    this.isRetranslateAvailable = function () {
        return _this.status == "FAILURE" && _this.selectedLab;
    };
    this.isTranslateAvailable = function () {
        return _this.status == "NOT_STARTED" && _this.selectedLab;
    };

    this.viewAvailable = _this.status == "SUCCESS";
    this.deleteAvailable = (isOwner || isLabHead) && _this.status == "SUCCESS" && countOfOwnedFilesTranslationData != 0;
};

ExperimentViewer.prototype.hidePopup = function () {
    $(this.containerSelector).modal("hide");
};

ExperimentViewer.prototype.showPopup = function () {
    $(this.containerSelector).modal("show");
};

ExperimentViewer.prototype.showCharts = function () {
    CommonLogger.log("Show charts button pressed");
    this.wnd.open(this.chartsUrl, "_blank");
    this.wnd.focus();
    this.hidePopup();
};

ExperimentViewer.prototype.retranslate = function () {
    CommonLogger.log("Retranslate button pressed");
    this.ExperimentTranslation.translate({id: this.experimentId, chargedLab: this.selectedLab});
    this.hidePopup();
};

ExperimentViewer.prototype.translate = function () {
    CommonLogger.log("Translate button pressed");
    this.ExperimentTranslation.translate({id: this.experimentId, chargedLab: this.selectedLab});
    this.hidePopup();
};

ExperimentViewer.prototype.remove = function () {
    CommonLogger.log("Delete button pressed");
    this.ExperimentTranslation.deleteTranslationData({id: this.experimentId});
    this.hidePopup();
};

ExperimentViewer.prototype.getStatus = function () {
    if (this.status == "SUCCESS") {
        return "All files translated successfully";
    } else if (this.status == "FAILURE") {
        return "Files were translated with errors";
    } else if (this.status == "IN_PROGRESS") {
        return "Translation in progress...";
    } else if (this.isOwner && !this.billLab) {
        return "Translation is not available. Please specify <a class='specifyLabNotificationLink' href='#/experiments/all/" + this.experimentId + "'>Lab to Send Billing</a>"
    } else {
        return "Not translated";
    }


};

var ExperimentDownloadLinkPopup = function (experiment, selector, AnonymousDownloadEmailer, downloadFiles, Security) {
    this._downloadFiles = downloadFiles([], experiment.id);
    this.popupSelector = selector;
    this.inviteHandler = function (item, callback) {
        CommonLogger.log(item.email);
        Security.invite({email: item.email}, function (invited) {
            callback(invited);
        });
    };
    this._emailLink = function (emails, publicEmail) {
        if (!this.isPublic()) {
            var i = emails.length - 1;
            while (i >= 0) {
                var email = emails[i].email;
                AnonymousDownloadEmailer.experiment({email: email, experiment: experiment.id});
                i--;
            }
        } else {
            AnonymousDownloadEmailer.experiment({email: publicEmail, experiment: experiment.id});
        }
    };
    this.isPublic = function () {
        return experiment.files > 0 && experiment.downloadLink != null && experiment.accessLevel == "PUBLIC";
    };
    this.selectCurrentInput = function (self) {
        var input = selector + " .item > input";
        $(input).focus();
        $(input).select();
    };
    this.downloadLink = (this.isPublic()) ? experiment.downloadLink : this._downloadFiles.url;
    this.email = null;
    this.sharedUsers = [];
    this.excludeEmails = [];
    this.emailLinkClicked = false;
};

var RestartSelectedProcessingRunsPopup = function (selector, restartCallback) {
    this.popupSelector = selector;
    this.restartCallback = restartCallback;
};

RestartSelectedProcessingRunsPopup.prototype.showPopup = function () {
    $(this.popupSelector).modal("show");
};
RestartSelectedProcessingRunsPopup.prototype.hidePopup = function () {
    $(this.popupSelector).modal("hide");
};

RestartSelectedProcessingRunsPopup.prototype.restart = function () {
    this.restartCallback();
    this.hidePopup();
};


ExperimentDownloadLinkPopup.prototype.showPopup = function () {
    var instance = this;
    $(this.popupSelector).modal("show");
    setTimeout(function () {
        instance.selectCurrentInput();
    }, 400);
};
ExperimentDownloadLinkPopup.prototype.hidePopup = function () {
    $(this.popupSelector).modal("hide");
};

ExperimentDownloadLinkPopup.prototype.emailLink = function () {
    this._emailLink(this.sharedUsers, this.email);
    if (!this.isPublic()) {
        this.hidePopup();
    } else {
        this.emailLinkClicked = true;
    }

};


function showExperimentDownloadConfirm(message, onOk) {
    var confirmOptions = {
        id: "experimentDownloadConfirm",
        title: "Study Download",
        message: message,
        dialogClass: "message-dialog warning",
        onOk: onOk
    };
    showConfirm(confirmOptions);
}


