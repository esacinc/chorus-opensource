package com.infoclinika.chorus.integration.skyline.api;

import edu.uw.gs.skyline.cloudapi.chromatogramgenerator.chromatogramrequest.ChromatogramRequestDocument;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.util.Random;
import java.util.Scanner;
import java.util.zip.Deflater;

import static com.infoclinika.analysis.storage.cloud.CloudStorageItemReference.CLOUD_REFERENCE_URL_SEPARATOR;


/**
 * @author Oleksii Tymchenko
 */
public class SkylineTestUtils {
    /**
     * * Utility methods ***
     */

    static String readRequestFromFile(String sampleRequestFile) {
        final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(sampleRequestFile);
        final Scanner scanner = new Scanner(is);
        final StringBuilder stringBuilder = new StringBuilder();
        while (scanner.hasNextLine()) {
            stringBuilder.append(scanner.nextLine());
        }
        return stringBuilder.toString();
    }

    public static String removeLastSlash(String baseUrl) {
        //if the URL ends with a slash, remove it
        if (baseUrl.endsWith(CLOUD_REFERENCE_URL_SEPARATOR)) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    public static void main(String[] args) {
        final Random random = new Random();
        final byte[] source = new byte[5_000_000];
        random.nextBytes(source);
        final long start = System.currentTimeMillis();
        final int totalRuns = 100;
        for(int i = 0; i < totalRuns; i++) {
            try {
                compress(source);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("Total time: " + (System.currentTimeMillis() - start) + "ms \n"
                + " Mean time: " + ((float)(System.currentTimeMillis() - start)) / totalRuns);
    }

    public static byte[] compress(byte[] bytes) throws IOException {
        if(bytes.length == 0) {
            return bytes;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try {
            deflater.setInput(bytes);
            deflater.finish();
            while (!deflater.finished()) {
                byte[] buffer = new byte[bytes.length];
                int count = deflater.deflate(buffer);
                byteArrayOutputStream.write(buffer, 0, count);
            }
            return byteArrayOutputStream.toByteArray();
        } finally {
            deflater.end();
        }
    }

    static ChromatogramRequestDocument parseRequest(String xmlRequest) {
        ChromatogramRequestDocument requestDocument;
        try {
            final Unmarshaller spectrumFilterUnmarshaller = JAXBContext.newInstance(ChromatogramRequestDocument.class).createUnmarshaller();
            requestDocument = (ChromatogramRequestDocument) spectrumFilterUnmarshaller.unmarshal(new StringReader(xmlRequest));
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to parse incoming request", e);
        }
        return requestDocument;
    }

    static String generateRequest(ChromatogramRequestDocument requestDocument) {
        StringWriter out = new StringWriter();
        try {
            final Marshaller spectrumFilterMarshaller = JAXBContext.newInstance(ChromatogramRequestDocument.class).createMarshaller();
            spectrumFilterMarshaller.marshal(requestDocument, out);
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to generate XML request from ChromatogramRequestDocument", e);
        }
        return out.toString();
    }
}
