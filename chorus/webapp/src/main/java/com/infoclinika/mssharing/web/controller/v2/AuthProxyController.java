package com.infoclinika.mssharing.web.controller.v2;

import com.infoclinika.mssharing.web.controller.v2.service.RestAuthClient;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.log4j.Logger;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import javax.inject.Inject;

@RestController
@RequestMapping("/v2/auth")
public class AuthProxyController {

    public static final Logger LOGGER = Logger.getLogger(AuthProxyController.class);

    @Inject
    private RestAuthClient restAuthClient;


    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleAccessDeniedException(Exception ex, WebRequest request) {
        LOGGER.info(HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<Object>(ex.getLocalizedMessage(), new HttpHeaders(), HttpStatus.UNAUTHORIZED);
    }

    @RequestMapping(value = "/cookie", method = RequestMethod.POST)
    public AuthCookieDTO getAuthCookie(@RequestBody CredentialsDTO credentials) {
        return restAuthClient.authenticateGetCookie(credentials.getUser(), credentials.getPassword());
    }



    @Data
    public static class CredentialsDTO {
        private String user;
        private String password;
    }

    @AllArgsConstructor
    public static class AuthCookieDTO {
        public String JSESSIONID;
    }


}
