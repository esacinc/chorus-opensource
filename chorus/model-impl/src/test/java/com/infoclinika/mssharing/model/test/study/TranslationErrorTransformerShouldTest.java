package com.infoclinika.mssharing.model.test.study;

import com.infoclinika.mssharing.model.helper.AbstractTest;
import com.infoclinika.mssharing.model.internal.read.TranslationErrorTransformer;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.testng.Assert.assertEquals;

/**
 * @author vladislav.kovchug
 */
public class TranslationErrorTransformerShouldTest extends AbstractTest {
    @Inject
    TranslationErrorTransformer translationErrorTransformer;

    @Test
    public void transform_null_to_null_test(){
        String result = translationErrorTransformer.transform(null);
        assertEquals(result, null);
    }

    @Test
    public void dont_transform_correct_messages(){
        String errorMessage = "File is omitted due to stop list.";
        String result = translationErrorTransformer.transform(errorMessage);
        assertEquals(result, errorMessage);
    }

    @Test void transform_stacktrace_to_readable_messages(){
        String errorMessage;
        String transformResult;

        errorMessage = "Unexpected error when translate file.Status : 500...com.infoclinika";
        transformResult = translationErrorTransformer.transform(errorMessage);
        assertEquals(transformResult, "Unexpected translation error. Please contact Chorus support.");

        errorMessage = "Runtime error translating the file: java.lang.IllegalStateException: ... Unknown ScanType ITMS + C ...";
        transformResult = translationErrorTransformer.transform(errorMessage);
        assertEquals(transformResult, "Centroid MS data type is not supported.");

        errorMessage = "Runtime error translating the file: java.lang.IllegalStateException: ... Q3MS ...";
        transformResult = translationErrorTransformer.transform(errorMessage);
        assertEquals(transformResult, "Q3MS filter type is not supported.");

        errorMessage = "Runtime error translating the file: java.lang.IllegalStateException: ... Unknown ScanType ITMS + P ESI. ...";
        transformResult = translationErrorTransformer.transform(errorMessage);
        assertEquals(transformResult, "ESI filter type is not supported.");

        errorMessage = "Runtime error translating the file: java.lang.IllegalStateException: ... Unknown filter type: FTMS + P NSI FULL MSX ...";
        transformResult = translationErrorTransformer.transform(errorMessage);
        assertEquals(transformResult, "MSX filter type is not supported.");

        errorMessage = "Runtime error translating the file: java.lang.IllegalStateException: ... File raw size does not match expected for file ...";
        transformResult = translationErrorTransformer.transform(errorMessage);
        assertEquals(transformResult, "File has not been uploaded fully. Please delete it and re-upload.");

        errorMessage = "Runtime error translating the file: java.lang.IllegalStateException: ... InvalidRawFileException: Cannot read functions ...";
        transformResult = translationErrorTransformer.transform(errorMessage);
        assertEquals(transformResult, "File is broken or not supported by our binding. Please delete it and re-upload.");

    }

}
