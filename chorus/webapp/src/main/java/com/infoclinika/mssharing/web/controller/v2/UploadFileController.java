package com.infoclinika.mssharing.web.controller.v2;

import com.infoclinika.mssharing.platform.web.security.RichUser;
import com.infoclinika.mssharing.web.controller.v2.service.UploadFileService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Inject;
import java.io.IOException;
import java.security.Principal;

@RestController
@RequestMapping("/v2/upload")
public class UploadFileController {


    @Inject
    private UploadFileService uploadFileService;


    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleAccessDeniedExeption(Exception ex, WebRequest request){
        return new ResponseEntity<>(ex.getLocalizedMessage(), new HttpHeaders(), HttpStatus.UNAUTHORIZED);

    }

    @RequestMapping(value ="/{instrumentId}/file", method = RequestMethod.POST)
    public ResponseEntity<?> uploadFile(Principal principal, @PathVariable("instrumentId") long instrumentId, @RequestParam("file") MultipartFile multipartFile) throws IOException {

        if(multipartFile.isEmpty()){
            return new ResponseEntity("Please select a file", HttpStatus.OK);
        }

        return uploadFileService.uploadFileToStorage(RichUser.get(principal).getId(), instrumentId, multipartFile);

    }

    @RequestMapping(value ="/{instrumentId}/files", method = RequestMethod.POST)
    public ResponseEntity<?> uploadFiles(Principal principal, @PathVariable("instrumentId") long instrumentId, @RequestParam("files") MultipartFile multipartFile){
        return ResponseEntity.ok().body(multipartFile.getName());

    }

}
