package com.infoclinika.mssharing.skyline.web.test;

import com.infoclinika.mssharing.skyline.web.test.help.SpringConfig;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author timofey.kasyanov
 *         date: 15.05.2014
 */
@ContextConfiguration(classes = SpringConfig.class)
public class SkylineAuthTest extends AbstractTestNGSpringContextTests {

    private static final String URL_DELIMITER = "/";
    private static final String SET_COOKIE_HEADER = "Set-Cookie";
    private static final String JSESSIONID = "JSESSIONID";
    private static final String AUTH_HEADER = "Authorization";

    @Value("${skyline.base.url}")
    private String baseUrl;

    @Value("${skyline.auth.path}")
    private String authPath;

    @Value("${skyline.content.path}")
    private String contentPath;

//    @Test
    public void testGetResourceWithAuthorization() throws IOException {

        final HttpClient client = new DefaultHttpClient();
        final HttpGet request = new HttpGet(baseUrl + URL_DELIMITER + contentPath);

        final String authenticationString = getBasicAuthenticationString("pavel.kaplin@gmail.com", "pwd");
        request.setHeader(AUTH_HEADER, authenticationString);

        final HttpResponse response = client.execute(request);
        final StatusLine statusLine = response.getStatusLine();
        final int statusCode = statusLine.getStatusCode();

        Assert.assertNotEquals(
                HttpServletResponse.SC_UNAUTHORIZED,
                statusCode,
                "Still unauthorized with proper authorize header"
        );

    }

//    @Test
    public void testGetResourceUnauthorized() throws IOException {

        final HttpClient client = new DefaultHttpClient();
        final HttpGet request = new HttpGet(baseUrl + URL_DELIMITER + contentPath);

        final HttpResponse response = client.execute(request);
        final StatusLine statusLine = response.getStatusLine();
        final int statusCode = statusLine.getStatusCode();

        Assert.assertEquals(HttpServletResponse.SC_FORBIDDEN, statusCode, "Status is not FORBIDDEN");

    }

//    @Test
    public void testAuthenticationFail() throws IOException {

        final HttpClient client = new DefaultHttpClient();
        final HttpGet request = new HttpGet(baseUrl + URL_DELIMITER + authPath);

        final String authenticationString =
                getBasicAuthenticationString("not-existing-username@skyline.sk", "password");
        request.setHeader(AUTH_HEADER, authenticationString);

        final HttpResponse response = client.execute(request);
        final StatusLine statusLine = response.getStatusLine();
        final int statusCode = statusLine.getStatusCode();

        Assert.assertEquals(HttpServletResponse.SC_UNAUTHORIZED, statusCode, "Status is not UNAUTHORIZED");

    }

//    @Test
    public void testAuthenticationSuccess() throws IOException {

        final HttpClient client = new DefaultHttpClient();
        final HttpGet request = new HttpGet(baseUrl + URL_DELIMITER + authPath);

        final String authenticationString = getBasicAuthenticationString("pavel.kaplin@gmail.com", "pwd");
        request.setHeader(AUTH_HEADER, authenticationString);

        final HttpResponse response = client.execute(request);
        final StatusLine statusLine = response.getStatusLine();
        final int statusCode = statusLine.getStatusCode();

        Assert.assertEquals(HttpServletResponse.SC_OK, statusCode, "Status is not OK");

        final Header setCookieHeader = response.getFirstHeader(SET_COOKIE_HEADER);
        Assert.assertNotNull(setCookieHeader, "Set-Cookie header was not set");

        final HeaderElement[] elements = setCookieHeader.getElements();
        final HeaderElement jsessionidElement = findJsessionidElement(elements);
        Assert.assertNotNull(jsessionidElement, "There is no JSESSIONID");

    }

    private String getBasicAuthenticationString(String username, String password){
        final String combined = username + ":" + password;
        final byte[] encoded = Base64.encodeBase64(combined.getBytes());
        return  "Basic " + new String(encoded);
    }

    private HeaderElement findJsessionidElement(HeaderElement[] elements){
        for (HeaderElement element : elements) {
            if(element.getName().equals(JSESSIONID)){
                return element;
            }
        }
        return null;
    }

}
