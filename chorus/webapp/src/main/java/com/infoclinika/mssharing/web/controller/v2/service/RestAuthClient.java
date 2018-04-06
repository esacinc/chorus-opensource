package com.infoclinika.mssharing.web.controller.v2.service;


import com.infoclinika.mssharing.web.controller.v2.AuthProxyController;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedList;
import java.util.List;

@Service
public class RestAuthClient {

    public static final Logger LOGGER = Logger.getLogger(RestAuthClient.class);

    @Value("${base.url}")
    private String baseUrl;



    public AuthProxyController.AuthCookieDTO authenticateGetCookie(String user, String password){
        HttpMessageConverter<MultiValueMap<String, ?>> formHttpMessageConverter = new FormHttpMessageConverter();

        HttpMessageConverter<String> stringHttpMessageConverternew = new StringHttpMessageConverter();

        List<HttpMessageConverter<?>> messageConverters = new LinkedList<HttpMessageConverter<?>>();

        messageConverters.add(formHttpMessageConverter);
        messageConverters.add(stringHttpMessageConverternew);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("j_username", user);
        map.add("j_password", password);

        String authURL = baseUrl + "/j_spring_security_check";
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.setMessageConverters(messageConverters);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, requestHeaders);

        ResponseEntity<String> result = restTemplate.exchange(authURL, HttpMethod.POST, entity, String.class);
        HttpHeaders respHeaders = result.getHeaders();

        if (respHeaders.getFirst("Location").contains("login_error")) {
            LOGGER.info("Wrong credentials", new RuntimeException());
        }

        String authcookie = respHeaders.get("Set-Cookie").stream()
                .filter(v -> v.contains("JSESSIONID"))
                .map(v->v.substring(0, v.indexOf(';')))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Failed to parse response"));
        return new AuthProxyController.AuthCookieDTO(authcookie.substring(authcookie.indexOf('=') + 1, authcookie.length()));
    }
}
