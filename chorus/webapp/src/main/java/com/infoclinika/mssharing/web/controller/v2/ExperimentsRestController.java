package com.infoclinika.mssharing.web.controller.v2;


import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.infoclinika.mssharing.dto.response.DictionaryDTO;
import com.infoclinika.mssharing.model.helper.ExperimentCreationHelper;
import com.infoclinika.mssharing.model.read.dto.details.ExperimentItem;
import com.infoclinika.mssharing.model.write.StudyManagement;
import com.infoclinika.mssharing.platform.model.common.items.DictionaryItem;
import com.infoclinika.mssharing.platform.web.security.RichUser;
import com.infoclinika.mssharing.web.controller.v2.service.ExperimentService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import javax.inject.Inject;
import java.security.Principal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping(value = "/v2/experiments")
public class ExperimentsRestController {


    @Inject
    private ExperimentService experimentService;



    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleAccessDeniedExeption(Exception ex, WebRequest request){
        return new ResponseEntity<>(ex.getLocalizedMessage(), new HttpHeaders(), HttpStatus.UNAUTHORIZED);

    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> anyUnknownException(Exception ex, WebRequest request) {
        return new ResponseEntity<>(
                ex.getLocalizedMessage(), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @RequestMapping(value = "/{experimentId}", method = RequestMethod.GET)
    public ExperimentService.ExperimentInfoDTO getExperimentInfoCertainFields(@PathVariable long experimentId, Principal principal) {
        return experimentService.returnExperimentInfo(RichUser.get(principal).getId(), experimentId);
    }
}
