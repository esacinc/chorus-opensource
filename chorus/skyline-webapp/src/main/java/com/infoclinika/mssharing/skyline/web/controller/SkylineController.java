package com.infoclinika.mssharing.skyline.web.controller;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import com.google.gson.Gson;
import com.infoclinika.analysis.storage.cloud.CloudStorageItemReference;
import com.infoclinika.integration.skyline.ExtractionContentExpert;
import com.infoclinika.msdata.image.MSRect;
import com.infoclinika.msdata.image.SpectrumType;
import com.infoclinika.mssharing.dto.request.ConfirmMultipartUploadDTO;
import com.infoclinika.mssharing.dto.request.UploadFilesDTORequest;
import com.infoclinika.mssharing.dto.response.*;
import com.infoclinika.mssharing.model.UploadUnavailable;
import com.infoclinika.mssharing.model.api.MSFunctionType;
import com.infoclinika.mssharing.model.helper.*;
import com.infoclinika.mssharing.model.read.DashboardReader;
import com.infoclinika.mssharing.model.read.DetailsReader;
import com.infoclinika.mssharing.model.read.FileLine;
import com.infoclinika.mssharing.model.read.ProjectLine;
import com.infoclinika.mssharing.model.read.dto.details.ExperimentItem;
import com.infoclinika.mssharing.model.write.*;
import com.infoclinika.mssharing.platform.model.common.items.DictionaryItem;
import com.infoclinika.mssharing.platform.model.common.items.InstrumentItem;
import com.infoclinika.mssharing.platform.model.helper.ExperimentCreationHelperTemplate.ExperimentTypeItem;
import com.infoclinika.mssharing.platform.model.helper.RegistrationHelperTemplate;
import com.infoclinika.mssharing.platform.model.read.DetailsReaderTemplate;
import com.infoclinika.mssharing.platform.model.read.Filter;
import com.infoclinika.mssharing.platform.model.write.ExperimentManagementTemplate;
import com.infoclinika.mssharing.platform.model.write.ExperimentManagementTemplate.Restriction;
import com.infoclinika.mssharing.skyline.processing.ExtractionPreProcessor;
import com.infoclinika.mssharing.skyline.processing.TranslatedFileData;
import com.infoclinika.mssharing.skyline.web.controller.request.ExperimentDetailsDTO;
import com.infoclinika.mssharing.skyline.web.controller.response.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.Principal;
import java.util.*;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.find;
import static com.google.common.collect.Lists.newArrayList;
import static com.infoclinika.mssharing.platform.web.security.RichUser.getUserId;

/**
 * @author Elena Kurilina
 */
@Controller
@RequestMapping("/api")
public class SkylineController {
    public static final String SIM_TYPE_SUBSTRING = " sim";
    public static final String[] SOURCE_COLORS = new String[]{"#0000b2", "#b20000", "#00b200", "#b200b2", "#b28c00", "#7c7c0b"};
    public static final String MY_CONTENTS = "my";
    public static final String SHARED_CONTENTS = "shared";
    public static final String PUBLIC_CONTENTS = "public";
    public static final String FRACTION_NUMBER_ANNOTATION = "fractionNumber";
    public static final String SAMPLE_ID_ANNOTATION = "sampleId";
    private static final Logger LOGGER = Logger.getLogger(SkylineController.class);
    @Value("${amazon.active.bucket}")
    private String activeBucket;

    @Inject
    private DetailsReader detailsReader;

    @Inject
    private DashboardReader dashboardReader;

    @Inject
    private ExperimentCreationHelper experimentCreationHelper;

    @Inject
    private StudyManagement studyManagement;

    @Inject
    private RegistrationHelperTemplate registrationHelper;

    @Inject
    private SecurityHelper securityHelper;

    @Inject
    private ExtractionPreProcessor extractionPreProcessor;

    @Inject
    private InstrumentManagement instrumentManagement;

    @Inject
    private RestHelper restHelper;

    @Inject
    private StoredObjectPaths storedObjectPaths;

    private static Set<DetailsReader.MSFunctionDetails> filterByType(Set<DetailsReader.MSFunctionDetails> functionDetails,
                                                                     final MSFunctionType targetType, final Predicate<String> translatedPathPredicate) {
        return Sets.filter(functionDetails, new Predicate<DetailsReader.MSFunctionDetails>() {
            @Override
            public boolean apply(DetailsReader.MSFunctionDetails input) {
                final boolean typeMatches = targetType.equals(input.type);
                final boolean predicateMatches = translatedPathPredicate == null || (translatedPathPredicate.apply(input.translatedPath));
                return typeMatches && predicateMatches;
            }
        });
    }


    @PostConstruct
    public void onPostConstruct() {
        LOGGER.info(" *** SKYLINE controller started with  activeBucket = " + activeBucket);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/chroextract/file/{fileId}")
    public void extractChromatograms(@PathVariable long fileId, HttpServletRequest request, HttpServletResponse response) {
        LOGGER.info("Obtaining skyline chromatogram response for file with ID: " + fileId + ". Headers:");
        printHeaders(request);
        final String xmlRequest = readStringsFromRequest(request);
        LOGGER.trace("Obtained XML request: " + xmlRequest);

        final TranslatedFileData translatedFileData = readTranslatedData(fileId);
        extractionPreProcessor.sendExtractRequest(translatedFileData, xmlRequest, response);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/chroextract/file/{fileId}/source/{chroSource}/precursor/{precursor}/{scanIndex}")
    public void extractChromatograms(@PathVariable long fileId, @PathVariable String chroSource,
                                     @PathVariable double precursor, @PathVariable int scanIndex,
                                     HttpServletRequest request, HttpServletResponse response) {
        LOGGER.info("Obtaining skyline chromatogram response for file with ID: " + fileId + ". Scan index: " + scanIndex + " . Headers:");

        final TranslatedFileData translatedFileData = readTranslatedData(fileId);
        extractionPreProcessor.sendExtractRequest(translatedFileData, chroSource, precursor, scanIndex, response);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/chroextract-drift/file/{fileId}/source/{chroSource}/precursor/{precursor}/{scanIndex}/drift/{driftTime:.+}")
    public void extractSpecificChromatogramsByDriftTime(@PathVariable long fileId, @PathVariable String chroSource,
                                                        @PathVariable double precursor, @PathVariable int scanIndex,
                                                        @PathVariable Double driftTime,
                                                        HttpServletRequest request, HttpServletResponse response) {
        LOGGER.info("Obtaining skyline chromatogram response for file with ID: " + fileId +
                ". Scan index: " + scanIndex + ". Drift time: " + driftTime + ".  Headers:");

        final TranslatedFileData translatedFileData = readTranslatedData(fileId);
        extractionPreProcessor.sendExtractRequestWithDriftTime(translatedFileData, chroSource, precursor, scanIndex, driftTime, response);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/chroextract-drift/file/{fileId}/source/{chroSource}/precursor/{precursor}/{scanIndex}")
    public void extractChromatogramsByDriftTime(@PathVariable long fileId, @PathVariable String chroSource,
                                                @PathVariable double precursor, @PathVariable int scanIndex,
                                                HttpServletRequest request, HttpServletResponse response) {
        LOGGER.info("Obtaining skyline chromatogram response for IMS file with ID: " + fileId +
                ". Scan index: " + scanIndex + ". Drift time: not specified.  Headers:");

        final TranslatedFileData translatedFileData = readTranslatedData(fileId);
        extractionPreProcessor.sendExtractRequestWithDriftTime(translatedFileData, chroSource, precursor, scanIndex, null, response);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/chroextract-stats/file/{fileId}")
    @ResponseBody
    public SkylineFileStatsResponse getFileStatsForSkyline(@PathVariable Long fileId) {

        LOGGER.info("Obtaining skyline stats file with ID: " + fileId);
        final TranslatedFileData translatedFileData = readTranslatedData(fileId);
        final Set<CloudStorageItemReference> ms1Contents = ExtractionContentExpert.parseMultipleRefs(translatedFileData.ms1Refs);
        final Set<CloudStorageItemReference> ms2Contents = ExtractionContentExpert.parseMultipleRefs(translatedFileData.ms2Refs);
        final Map<Double, CloudStorageItemReference> ms1FnsByDriftTime = ExtractionContentExpert.layoutByDriftTime(ms1Contents);
        final Map<Double, CloudStorageItemReference> ms2FnsByDriftTime = ExtractionContentExpert.layoutByDriftTime(ms2Contents);
        final Set<Double> finalDriftTimes = new TreeSet<>();
        finalDriftTimes.addAll(ms1FnsByDriftTime.keySet());
        finalDriftTimes.addAll(ms2FnsByDriftTime.keySet());
        return new SkylineFileStatsResponse(new ArrayList<>(finalDriftTimes));
    }

    // -- Experiment creation and details ---

    @RequestMapping(method = RequestMethod.GET, value = "/handshake")
    @ResponseStatus(HttpStatus.OK)
    public void handshake(Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("REST: handshake confirmed for user ID: " + userId);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/contents")
    @ResponseBody
    public DashboardReader.FullFolderStructure readFolderStructure(Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("Skyline: obtaining whole contents for user ID: " + userId);
        return dashboardReader.readFolderStructure(userId);
    }

    @RequestMapping(value = "/ew/available-projects", method = RequestMethod.GET)
    @ResponseBody
    public List<ProjectLine> getProjectsAllowedForWriting(Principal principal) {
        return newArrayList(dashboardReader.readProjectsAllowedForWriting(getUserId(principal)));
    }

    @RequestMapping("/ew/available-labs")
    @ResponseBody
    public ImmutableSet<RegistrationHelperTemplate.LabItem> userLabsWithEnabledFeature(Principal principal) {
        if (principal == null) {
            return ImmutableSet.of();
        }
        final long userId = getUserId(principal);
        LOGGER.info("REST API: obtaining available labs for user ID: " + userId);
        final String feature = "billing";
        final ImmutableSortedSet<RegistrationHelperTemplate.LabItem> labs = registrationHelper.availableLabs();
        return from(securityHelper.getUserDetails(userId).labs)
                .transform(new Function<Long, RegistrationHelperTemplate.LabItem>() {
                    @Override
                    public RegistrationHelperTemplate.LabItem apply(final Long input) {
                        return find(labs, new Predicate<RegistrationHelperTemplate.LabItem>() {
                            @Override
                            public boolean apply(RegistrationHelperTemplate.LabItem item) {
                                return item.id == input;
                            }
                        });
                    }
                })
                .filter(new Predicate<RegistrationHelperTemplate.LabItem>() {
                    @Override
                    public boolean apply(RegistrationHelperTemplate.LabItem input) {
                        return securityHelper.isFeatureEnabledForLab(feature, input.id);
                    }
                })
                .toSet();
    }

    @RequestMapping("/ew/instrumentModels")
    @ResponseBody
    public List<DictionaryItem> getInstrumentModels(Principal principal, @RequestParam(required = false) Long lab) {
        final long userId = getUserId(principal);
        LOGGER.info("REST API: obtaining instrument models for user ID " + userId + " and lab " + lab);

        return experimentCreationHelper.availableInstrumentModels(getUserId(principal), lab);
    }

    @RequestMapping("/ew/instruments")
    @ResponseBody
    public List<InstrumentItem> getInstruments(@RequestParam long instrumentModel, Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("REST API: obtaining instrument models for user ID " + userId + " and instrument model " + instrumentModel);
        return experimentCreationHelper.availableInstrumentsByModel(userId, instrumentModel);
    }

    @RequestMapping(value = "/ew/available-files", method = RequestMethod.GET)
    @ResponseBody
    public List<com.infoclinika.mssharing.platform.model.common.items.FileItem> getFiles(@RequestParam long specie, @RequestParam(required = false) Long instrument,
                                                                                         @RequestParam(required = false) Long model, @RequestParam(required = false) Long lab, Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("REST API: obtaining available files for user ID " + userId + ", species ID " + specie
                + ", instrument ID " + instrument + ", instrument model ID " + model + ", lab ID " + lab);
        if (instrument != null) {
            return experimentCreationHelper.availableFilesByInstrument(userId, specie, instrument);
        }
        return experimentCreationHelper.availableFilesByInstrumentModel(userId, specie, model, lab);
    }

    @RequestMapping(value = "/ew/{experiment}/files", method = RequestMethod.GET)
    @ResponseBody
    public SortedSet<com.infoclinika.mssharing.platform.model.common.items.FileItem> getExperimentFiles(@PathVariable long experiment, Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("REST API: obtaining experiment files for user ID " + userId + ", experiment ID " + experiment);
        return dashboardReader.readFileItemsByExperiment(userId, experiment);
    }

    @RequestMapping("/ew/experimentTypes")
    @ResponseBody
    public ImmutableSortedSet<ExperimentTypeItem> getExperimentTypes() {
        LOGGER.info("REST API: obtaining experiment types");
        return experimentCreationHelper.experimentTypes();
    }

    @RequestMapping("/ew/experimentLabelTypes")
    @ResponseBody
    public Set<ExperimentCreationHelper.ExperimentLabelTypeItem> getExperimentLabelTypes() {
        LOGGER.info("REST API: obtaining experiment label types");
        return experimentCreationHelper.experimentLabelTypes();
    }

    @RequestMapping("/ew/experimentLabels/{id}")
    @ResponseBody
    public Set<ExperimentCreationHelper.ExperimentLabelItem> getExperimentLabels(@PathVariable long id) {
        LOGGER.info("REST API: obtaining experiment labels for label type ID " + id);
        return Sets.newHashSet(experimentCreationHelper.experimentLabels(id));
    }

    @RequestMapping("/ew/experimentLabels")
    @ResponseBody
    public Set<ExperimentCreationHelper.ExperimentLabelItem> getExperimentLabels() {
        LOGGER.info("REST API: obtaining experiment labels");
        return Sets.newHashSet(experimentCreationHelper.experimentLabels());
    }

    @RequestMapping("/ew/species")
    @ResponseBody
    public ImmutableSet<DictionaryItem> species() {
        LOGGER.info("REST API: obtaining experiment types");
        return experimentCreationHelper.species();
    }

    @RequestMapping(value = "/ew/species/{id}", method = RequestMethod.GET)
    @ResponseBody
    public DictionaryItem getSpecie(@PathVariable long id) {
        LOGGER.info("REST API: obtaining species details for ID " + id);
        return experimentCreationHelper.specie(id);
    }

    @RequestMapping(value = "/ew/save", method = RequestMethod.POST)
    @ResponseBody
    public ExperimentSavedResponse save(@RequestBody ExperimentDetailsDTO experiment, Principal principal) {
        long userId = getUserId(principal);
        long experimentId;
        //TODO: Remove when billing will be enabled finally
        final Long billLab = Optional.fromNullable(experiment.billLab).or(0l);
        final Restriction convertedRestriction = new Restriction(
                experiment.restriction.instrumentModel,
                experiment.restriction.instrument == null ? Optional.absent() : Optional.of(experiment.restriction.instrument)
        );
        final ExperimentInfo experimentInfo = new ExperimentInfo.Builder()
                .name(experiment.info.name)
                .description(experiment.info.description)
                .experimentType(experiment.type)
                .specie(experiment.info.specie)
                .project(experiment.project)
                .lab(experiment.lab)
                .billLab(billLab)
                .is2dLc(experiment.twoDLCEnabled)
                .restriction(convertedRestriction)
                .factors(experiment.factors)
                .files(convertFiles(experiment.files))
                .bounds(experiment.bounds)
                .lockMasses(experiment.lockMasses)
                .sampleTypesCount(experiment.mixedSamples)
                .experimentLabels(experiment.info.experimentLabels)
                .build();

        if (experiment.id == null) {
            LOGGER.info("REST API: creating experiment for user ID = " + userId + " and data: " + experiment);
            experimentId = studyManagement.createExperiment(userId, experimentInfo);
        } else {
            LOGGER.info("REST API: updating experiment for user ID = " + userId + " and data: " + experiment);
            experimentId = experiment.id;
            studyManagement.updateExperiment(userId, experimentId, experimentInfo);
        }
        return new ExperimentSavedResponse(experimentId);
    }

    private List<FileItem> convertFiles(List<ExperimentFileItemDTO> files) {
        return new ArrayList<>(Lists.transform(files, input -> new FileItem(
                input.id,
                false,
                input.fractionNumber != null ? input.fractionNumber : 0,
                new ExperimentPreparedSampleItem(
                        input.preparedSample.name,
                        Sets.newHashSet(Collections2.transform(input.preparedSample.samples, input1 -> new ExperimentSampleItem(input1.name, ExperimentSampleTypeItem.valueOf(input1.type.name()), input1.factorValues)))
                )
        )));
    }

    @RequestMapping(value = "/ew/details/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ExperimentDetailsDTO getDetails(@PathVariable final Long id, Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("REST API: obtaining experiment details for user ID = " + userId);
        ExperimentItem details = detailsReader.readExperiment(userId, id);
        final Long instrument = details.instrument.isPresent() ? details.instrument.get() : null;
        ExperimentDetailsDTO result = new ExperimentDetailsDTO();
        result.info = new ExperimentInfo.Builder()
                .name(details.name)
                .description(details.description)
                .experimentType(details.experimentType)
                .specie(details.specie)
                .experimentLabels(new ExperimentLabelsInfo(
                        details.labels.lightLabels,
                        details.labels.mediumLabels,
                        details.labels.heavyLabels,
                        details.labels.specialLabels))
                .sampleTypesCount(details.sampleTypesCount)
                .build();
        result.restriction = new RestrictionDTO(details.technologyType, details.instrumentVendorId, details.instrumentModel, instrument);
        result.project = details.project;
        result.twoDLCEnabled = details.is2dLc;
        result.ownerEmail = details.ownerEmail;
        result.lab = details.lab;
        result.labHead = details.labHead;
        result.accessLevel = details.accessLevel;
        result.billLab = details.billLab;
        result.mixedSamples = details.sampleTypesCount;

        final ImmutableSortedSet<ExperimentTypeItem> experimentTypeItems = experimentCreationHelper.experimentTypes();
        final Optional<ExperimentTypeItem> typeItemOptional = FluentIterable.from(experimentTypeItems).firstMatch(type -> type.id == details.experimentType);
        if (typeItemOptional.isPresent()) {
            result.labelsEnabled = typeItemOptional.get().allowLabels;
        }

        result.factors = from(details.factors).transform(new Function<DetailsReaderTemplate.MetaFactorTemplate, ExperimentManagementTemplate.MetaFactorTemplate>() {
            @Nullable
            @Override
            public ExperimentManagementTemplate.MetaFactorTemplate apply(DetailsReaderTemplate.MetaFactorTemplate input) {
                return new ExperimentManagementTemplate.MetaFactorTemplate(input.name, input.units, input.isNumeric, input.experimentId);
            }
        }).toList();

        result.files = from(details.files).transform(toDetailsFiles()).toList();

        result.type = details.experimentType;
        result.id = id;
        result.bounds = details.bounds;
        result.lockMasses = details.lockMasses;
        result.numberOfProteinSearches = details.numberOfProteinSearches;
        result.labName = details.labName;
        return result;
    }

    @RequestMapping(value = "/ew/levels/{experimentId}", method = RequestMethod.GET)
    @ResponseBody
    public Set<DashboardReader.ExperimentLevelItem> getExperimentLevels(@PathVariable final long experimentId, Principal principal) {
        return dashboardReader.readExperimentLevels(getUserId(principal), experimentId);
    }

    // --- Desktop Uploader
    @RequestMapping(value = "/du/upload_config", method = RequestMethod.POST)
    @ResponseBody
    public UploadConfigDTO getUploadConfig(Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("REST API: obtaining uploading config for user ID " + userId);

        return new UploadConfigDTO(
                storedObjectPaths.getAmazonKey(),
                storedObjectPaths.getAmazonSecret(),
                storedObjectPaths.getRawFilesBucket()
        );
    }

    @RequestMapping(value = "/du/instruments", method = RequestMethod.GET)
    @ResponseBody
    public Set<InstrumentItem> getInstruments(Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("REST API: obtaining instrument models for user ID " + userId);

        return dashboardReader.readInstrumentsWhereUserIsOperator(userId);
    }

    @RequestMapping(value = "/du/instrument_files/{instrumentId}", method = RequestMethod.GET)
    @ResponseBody
    public Set<FileLine> getInstrumentFiles(@PathVariable Long instrumentId, Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("REST API: obtaining instrument files for user ID " + userId + " and instrument ID " + instrumentId);

        return dashboardReader.readFilesByInstrument(userId, instrumentId);
    }

    @RequestMapping(value = "/du/default_specie", method = RequestMethod.GET)
    @ResponseBody
    public DictionaryItem getDefaultSpecie(Principal principal) {
        getUserId(principal);
        LOGGER.info("REST API: obtaining default specie");

        return experimentCreationHelper.defaultSpecie();
    }

    @RequestMapping(value = "/du/unfinished_uploads", method = RequestMethod.GET)
    @ResponseBody
    public Set<FileLine> getUnfinishedUploads(Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("REST API: obtaining unfinished uploads for user ID " + userId);

        return dashboardReader.readUnfinishedFiles(userId);
    }

    @RequestMapping(value = "/du/delete_upload/{fileId}", method = RequestMethod.GET)
    @ResponseBody
    public DeleteUploadDTO deleteUpload(@PathVariable Long fileId, Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("REST API: delete upload for user ID " + userId + " and file ID " + fileId);
        instrumentManagement.cancelUpload(userId, fileId);

        return new DeleteUploadDTO(true);
    }

    @RequestMapping(value = "/du/upload_request", method = RequestMethod.POST)
    @ResponseBody
    public UploadFilesDTOResponse uploadRequest(@RequestBody UploadFilesDTORequest request, Principal principal) {
        final long userId = getUserId(principal);
        final long instrumentId = request.getInstrument();

        if (!restHelper.canUploadForInstrument(instrumentId)) {
            return new UploadFilesDTOResponse(
                    instrumentId,
                    new ArrayList<UploadFilesDTOResponse.UploadFile>()
            );
        }

        final List<UploadFilesDTOResponse.UploadFile> responseFiles = newArrayList();
        final List<UploadFilesDTORequest.UploadFile> files = request.getFiles();

        for (UploadFilesDTORequest.UploadFile file : files) {
            Long fileItemId = instrumentManagement.findUploadResumableFile(userId, instrumentId, file.getName());
            final boolean started = fileItemId != null;
            final String path = storedObjectPaths.rawFilePath(userId, instrumentId, file.getName()).getPath();

            if (fileItemId == null) {
                fileItemId = instrumentManagement.createFile(
                        userId,
                        instrumentId,
                        new FileMetaDataInfo(file.getName(), file.getSize(), file.getLabels(), path, file.getSpecie(), file.isArchive(), false)
                );
            }

            final UploadFilesDTOResponse.UploadFile uploadFile
                    = new UploadFilesDTOResponse.UploadFile(file.getName(), fileItemId, path);
            uploadFile.setStarted(started);
            responseFiles.add(uploadFile);
        }

        return new UploadFilesDTOResponse(instrumentId, responseFiles);
    }

    // --- MY contents ---

    @RequestMapping(value = "/du/simple_upload_request", method = RequestMethod.POST)
    @ResponseBody
    public SimpleUploadFilesDTOResponse simpleUploadRequest(@RequestBody UploadFilesDTORequest request, Principal principal) {
        final long userId = getUserId(principal);
        final long instrumentId = request.getInstrument();

        long uploadSize = 0;
        for (UploadFilesDTORequest.UploadFile uploadFile : request.getFiles()) {
            uploadSize += uploadFile.getSize();
        }
        instrumentManagement.checkCanUploadMore(instrumentId, uploadSize);

        if (!restHelper.canUploadForInstrument(instrumentId)) {
            return new SimpleUploadFilesDTOResponse(
                    instrumentId,
                    new ArrayList<SimpleUploadFilesDTOResponse.UploadFilePath>()
            );
        }

        final List<SimpleUploadFilesDTOResponse.UploadFilePath> responseFiles = newArrayList();
        final List<UploadFilesDTORequest.UploadFile> files = request.getFiles();

        for (UploadFilesDTORequest.UploadFile file : files) {
            final String path = storedObjectPaths.rawFilePath(userId, instrumentId, file.getName()).getPath();
            final SimpleUploadFilesDTOResponse.UploadFilePath uploadFilePath =
                    new SimpleUploadFilesDTOResponse.UploadFilePath(path);

            responseFiles.add(uploadFilePath);
        }

        return new SimpleUploadFilesDTOResponse(instrumentId, responseFiles);
    }

    @RequestMapping(value = "/du/complete_upload_request", method = RequestMethod.POST)
    @ResponseBody
    public CompleteUploadDTO completeUpload(@RequestBody ConfirmMultipartUploadDTO request, Principal principal) {
        final long userId = getUserId(principal);
        try {
            instrumentManagement.completeMultipartUpload(
                    userId,
                    request.getFileId(),
                    request.getRemoteDestination()
            );
        } catch (UploadUnavailable e) {
            LOGGER.error("Upload unavailable", e);
            return new CompleteUploadDTO(false);
        }

        return new CompleteUploadDTO(true);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/contents/" + MY_CONTENTS)
    @ResponseBody
    public SkylineMyFolderStructureResponse readMyFolderStructure(Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("Skyline: obtaining OWN contents for user ID: " + userId);
        final DashboardReader.FolderStructure result = dashboardReader.readFolderStructure(userId, Filter.MY);
        return new SkylineMyFolderStructureResponse(result.projects, result.experiments, result.files);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/contents/" + MY_CONTENTS + "/projects")
    @ResponseBody
    public ProjectsStructureResponse readMyProjectsFolderStructure(Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("Skyline: obtaining OWN PROJECTS for user ID: " + userId);
        final SortedSet<DashboardReader.ProjectStructure> result = dashboardReader.readProjectsOnlyStructure(userId, Filter.MY);
        return new ProjectsStructureResponse(result);
    }

    // --- SHARED contents ---

    @RequestMapping(method = RequestMethod.GET, value = "/contents/" + MY_CONTENTS + "/experiments")
    @ResponseBody
    public ExperimentsStructureResponse readMyExperimentsFolderStructure(Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("Skyline: obtaining OWN EXPERIMENTS for user ID: " + userId);
        final SortedSet<DashboardReader.ExperimentStructure> result = dashboardReader.readExperimentsOnlyStructure(userId, Filter.MY);
        return new ExperimentsStructureResponse(result);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/contents/" + MY_CONTENTS + "/files")
    @ResponseBody
    public FilesStructureResponse readMyFilesFolderStructure(Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("Skyline: obtaining OWN FILES for user ID: " + userId);
        final SortedSet<DashboardReader.UploadedFile> result = dashboardReader.readFilesOnlyStructure(userId, Filter.MY);
        return new FilesStructureResponse(result);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/contents/" + SHARED_CONTENTS)
    @ResponseBody
    public SkylineSharedFolderStructureResponse readSharedFolderStructure(Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("Skyline: obtaining SHARED contents for user ID: " + userId);
        final DashboardReader.FolderStructure result = dashboardReader.readFolderStructure(userId, Filter.SHARED_WITH_ME);
        return new SkylineSharedFolderStructureResponse(result.projects, result.experiments, result.files);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/contents/" + SHARED_CONTENTS + "/projects")
    @ResponseBody
    public ProjectsStructureResponse readSharedProjectsFolderStructure(Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("Skyline: obtaining SHARED PROJECTS for user ID: " + userId);
        final SortedSet<DashboardReader.ProjectStructure> result = dashboardReader.readProjectsOnlyStructure(userId, Filter.SHARED_WITH_ME);
        return new ProjectsStructureResponse(result);
    }


    // --- PUBLIC contents ---

    @RequestMapping(method = RequestMethod.GET, value = "/contents/" + SHARED_CONTENTS + "/experiments")
    @ResponseBody
    public ExperimentsStructureResponse readSharedExperimentsFolderStructure(Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("Skyline: obtaining SHARED EXPERIMENTS for user ID: " + userId);
        final SortedSet<DashboardReader.ExperimentStructure> result = dashboardReader.readExperimentsOnlyStructure(userId, Filter.SHARED_WITH_ME);
        return new ExperimentsStructureResponse(result);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/contents/" + SHARED_CONTENTS + "/files")
    @ResponseBody
    public FilesStructureResponse readSharedFilesFolderStructure(Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("Skyline: obtaining SHARED FILES for user ID: " + userId);
        final SortedSet<DashboardReader.UploadedFile> result = dashboardReader.readFilesOnlyStructure(userId, Filter.SHARED_WITH_ME);
        return new FilesStructureResponse(result);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/contents/" + PUBLIC_CONTENTS)
    @ResponseBody
    public SkylinePublicFolderStructureResponse readPublicFolderStructure(Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("Skyline: obtaining PUBLIC contents for user ID: " + userId);
        final DashboardReader.FolderStructure result = dashboardReader.readFolderStructure(userId, Filter.PUBLIC);
        return new SkylinePublicFolderStructureResponse(result.projects, result.experiments, result.files);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/contents/" + PUBLIC_CONTENTS + "/projects")
    @ResponseBody
    public ProjectsStructureResponse readPublicProjectsFolderStructure(Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("Skyline: obtaining PUBLIC PROJECTS for user ID: " + userId);
        final SortedSet<DashboardReader.ProjectStructure> result = dashboardReader.readProjectsOnlyStructure(userId, Filter.PUBLIC);
        return new ProjectsStructureResponse(result);
    }

    // --- Details by project and experiment

    @RequestMapping(method = RequestMethod.GET, value = "/contents/" + PUBLIC_CONTENTS + "/experiments")
    @ResponseBody
    public ExperimentsStructureResponse readPublicExperimentsFolderStructure(Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("Skyline: obtaining PUBLIC EXPERIMENTS for user ID: " + userId);
        final SortedSet<DashboardReader.ExperimentStructure> result = dashboardReader.readExperimentsOnlyStructure(userId, Filter.PUBLIC);
        return new ExperimentsStructureResponse(result);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/contents/" + PUBLIC_CONTENTS + "/files")
    @ResponseBody
    public FilesStructureResponse readPublicFilesFolderStructure(Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("Skyline: obtaining PUBLIC FILES for user ID: " + userId);
        final SortedSet<DashboardReader.UploadedFile> result = dashboardReader.readFilesOnlyStructure(userId, Filter.PUBLIC);
        return new FilesStructureResponse(result);
    }

    /* Charts */
    //todo[tymchenko]: copied from ChartsController; refactor to unite APIs

    @RequestMapping(method = RequestMethod.GET, value = "/contents/projects/{id}/experiments")
    @ResponseBody
    public ExperimentsStructureResponse readExperimentsByProjectStructure(@PathVariable final Long id, Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("Skyline: obtaining EXPERIMENTS for user ID " + userId + " and PROJECT ID: " + id);
        final SortedSet<DashboardReader.ExperimentStructure> result = dashboardReader.readExperimentsOnlyStructureByProject(userId, id);
        return new ExperimentsStructureResponse(result);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/contents/experiments/{id}/files")
    @ResponseBody
    public FilesStructureResponse readFilesByExperimentStructure(@PathVariable final Long id, Principal principal) {
        final long userId = getUserId(principal);
        LOGGER.info("Skyline: obtaining FILES contents for user ID " + userId + " and EXPERIMENT ID: " + id);
        final SortedSet<DashboardReader.UploadedFile> result = dashboardReader.readFilesStructureByExperiment(userId, id);
        return new FilesStructureResponse(result);
    }

    private String[] generateColors(Long[] conditionsIds) {
        final String[] generatedColors = new String[conditionsIds.length];

        final Set<Long> uniqueConditionSet = new HashSet<>(Arrays.asList(conditionsIds));
        final LinkedList<Long> uniqueConditionList = new LinkedList<>(uniqueConditionSet);
        Collections.sort(uniqueConditionList);

        for (int i = 0; i < conditionsIds.length; i++) {
            Long conditionsId = conditionsIds[i];
            final int index = uniqueConditionList.indexOf(conditionsId);
            final int colorIndex = index % SOURCE_COLORS.length;
            generatedColors[i] = SOURCE_COLORS[colorIndex];
        }
        return generatedColors;
    }

    @ExceptionHandler(value = Exception.class)
    public void defaultErrorHandler(HttpServletRequest req, HttpServletResponse response, Exception e) throws Exception {
        sendExceptionToResponse(response, e);
    }

    private <REQUEST_CLASS> REQUEST_CLASS parseRequest(HttpServletRequest request, Class<REQUEST_CLASS> clazz) {
        final String rawJson = readStringsFromRequest(request);
        return new Gson().fromJson(rawJson, clazz);
    }

    private void printHeaders(HttpServletRequest request) {
        try {
            final Enumeration headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                final Object rawHeaderName = headerNames.nextElement();
                LOGGER.info(" > " + rawHeaderName + ": " + request.getHeader((String) rawHeaderName));
            }
        } catch (Exception e) {
            LOGGER.warn("Cannot print headers for the request", e);
        }
    }

    private PreciseMSRect addBordersToSmallWindow(PreciseMSRect originalRange, SpectrumType type) {

        final MSRect border = new MSRect(50000, 50000, 50000, 0);

        PreciseMSRect rangeWithBorders;
        if (SpectrumType.BPI_CHROMATOGRAM.equals(type) || SpectrumType.TIC_CHROMATOGRAM.equals(type)) {
            //for chromatogram
            if (originalRange.startRt > 0 && originalRange.endRt > 0) {
                LOGGER.debug("Adding borders to rect: " + originalRange);
                rangeWithBorders = new PreciseMSRect(originalRange.startRt - border.startRt, originalRange.endRt + border.endRt,
                        originalRange.startMz, originalRange.endMz);
            } else {
                LOGGER.debug("SKIPPED adding borders to the rect due to unlimited border from one of the sides: " + originalRange);
                rangeWithBorders = originalRange;
            }
        } else {
            //for spectrum
            if (originalRange.startMz > 0 && originalRange.endMz > 0) {
                LOGGER.debug("Adding borders to rect: " + originalRange);
                rangeWithBorders = new PreciseMSRect(originalRange.startRt, originalRange.endRt,
                        originalRange.startMz - border.startMz, originalRange.endMz + border.endMz);
            } else {
                LOGGER.debug("SKIPPED adding borders to the rect due to unlimited border from one of the sides: " + originalRange);
                rangeWithBorders = originalRange;
            }
        }
        return rangeWithBorders;
    }

    private Set<String> toRefs(Iterable<DetailsReader.MSFunctionDetails> functions) {
        return Sets.newHashSet(Iterables.transform(functions, new Function<DetailsReader.MSFunctionDetails, String>() {
            @Override
            public String apply(DetailsReader.MSFunctionDetails input) {
                return new CloudStorageItemReference(activeBucket, input.translatedPath).asDelimitedPath();
            }
        }));
    }

    private String readStringsFromRequest(HttpServletRequest request) {
        final StringBuilder requestAggregator = new StringBuilder();
        String line;
        try {
            BufferedReader br = request.getReader();
            while ((line = br.readLine()) != null) {
                requestAggregator.append(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return requestAggregator.toString();
    }

    private void sendExtractRequest(final String requestContents, final String apiURL, final HttpServletResponse response) {
        LOGGER.debug("Sending test request to " + apiURL);
        LOGGER.trace("Request is " + requestContents);
        final HttpClient client = new DefaultHttpClient();
        final HttpPost post = new HttpPost(apiURL);
        try {


            final HttpEntity requestEntity = new ByteArrayEntity(requestContents.getBytes("UTF-8"));
            post.setEntity(requestEntity);
            final HttpResponse serviceResponse = client.execute(post);
            final int statusCode = serviceResponse.getStatusLine().getStatusCode();
            LOGGER.info("Got the response from service. Status code: " + statusCode
                    + ". Details: " + serviceResponse.getStatusLine());
            if (statusCode != HttpStatus.OK.value()) {
                throw new RuntimeException("Extraction server is not available");
            }
            final HttpEntity responseEntity = serviceResponse.getEntity();

            LOGGER.info("Convering the results to bytes");

            final byte[] resultInBytes = EntityUtils.toByteArray(responseEntity);
            LOGGER.info("Result size is " + resultInBytes.length);
            response.setContentLength(resultInBytes.length);

            final ServletOutputStream outputStream = response.getOutputStream();
            IOUtils.copy(new ByteArrayInputStream(resultInBytes), outputStream);
            response.setStatus(200);

            outputStream.flush();
        } catch (Exception e) {
            sendExceptionToResponse(response, e);
            LOGGER.error("Could not process the request for URL: " + apiURL, e);
//            throw new RuntimeException(e);
        }
    }

    private void sendExceptionToResponse(HttpServletResponse response, Exception e) {
        final StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append("<ChorusErrorResponse>");
        responseBuilder.append("<StackTrace>");
        responseBuilder.append(ExceptionUtils.getStackTrace(e));
        responseBuilder.append("</StackTrace>");
        responseBuilder.append("</ChorusErrorResponse>");

        response.setStatus(500);
        try {
            response.getWriter().write(responseBuilder.toString());
            response.flushBuffer();
        } catch (IOException ignored) {
            LOGGER.warn("Could not fill the error stacktrace ", ignored);
        }
    }

    private Function<DetailsReaderTemplate.FileItemTemplate, ExperimentFileItemDTO> toDetailsFiles() {
        return input -> {
            final com.infoclinika.mssharing.model.read.dto.details.FileItem fileItem = (com.infoclinika.mssharing.model.read.dto.details.FileItem) input;
            final ExperimentFileItemDTO dto = new ExperimentFileItemDTO();

            dto.id = fileItem.id;
            dto.date = fileItem.uploadDate;
            dto.fractionNumber = fileItem.fractionNumber;
            dto.labels = fileItem.labels;
            dto.preparedSample = new ExperimentFileItemDTO.PreparedSample();
            final ExperimentPreparedSampleItem preparedSample = fileItem.preparedSample;
            dto.preparedSample.name = preparedSample.name;
            dto.preparedSample.samples = Lists.newArrayList(Collections2.transform(preparedSample.samples, new Function<ExperimentSampleItem, ExperimentFileItemDTO.Sample>() {
                @Override
                public ExperimentFileItemDTO.Sample apply(ExperimentSampleItem sampleInput) {
                    final ExperimentFileItemDTO.Sample sample = new ExperimentFileItemDTO.Sample();
                    sample.name = sampleInput.name;
                    sample.factorValues = Lists.newArrayList(sampleInput.factorValues);
                    sample.type = ExperimentFileItemDTO.SampleType.valueOf(sampleInput.type.name());
                    return sample;
                }
            }));

            return dto;
        };
    }

    private TranslatedFileData readTranslatedData(long fileId) {
        //todo[tymchenko]: security
        final DetailsReader.MSFunctions msFunctions = detailsReader.readFunctionsForFile(-1, fileId);
        final Set<DetailsReader.MSFunctionDetails> functionDetails = msFunctions.functionDetails;
        final Iterable<DetailsReader.MSFunctionDetails> ms1Functions = filterByType(functionDetails, MSFunctionType.MS, new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return !input.toLowerCase().contains(SIM_TYPE_SUBSTRING);
            }
        });

        final Set<DetailsReader.MSFunctionDetails> simFunctions = filterByType(functionDetails, MSFunctionType.MS, new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return input.toLowerCase().contains(SIM_TYPE_SUBSTRING);
            }
        });

        final Set<DetailsReader.MSFunctionDetails> ms2Functions = filterByType(functionDetails, MSFunctionType.MS2, null);
        final Set<String> ms1Refs = toRefs(ms1Functions);
        final Set<String> ms2Refs = toRefs(ms2Functions);
        final Set<String> simRefs = toRefs(simFunctions);

        return new TranslatedFileData(ms1Refs, ms2Refs, simRefs);
    }

    /*
     * MS Rectangle holding the converted values (i.e. multiplied by 100K), but still in original data types
     */
    private static final class PreciseMSRect {
        public final float startRt;
        public final float endRt;
        public final double startMz;
        public final double endMz;

        private PreciseMSRect(float startRt, float endRt, double startMz, double endMz) {
            this.startRt = startRt;
            this.endRt = endRt;
            this.startMz = startMz;
            this.endMz = endMz;
        }
    }


}
