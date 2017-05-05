package com.infoclinika.mssharing.skyline.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;

/**
 * @author timofey.kasyanov
 *         date: 14.05.2014
 */
@Controller
@RequestMapping("/authenticate")
public class SkylineAuthController {

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public void authentication(Principal principal, HttpServletResponse response) throws IOException {

        if(principal == null){
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
        }

    }

}
