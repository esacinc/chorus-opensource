package com.infoclinika.mssharing.model.internal.read;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author vladislav.kovchug
 */
@Service
public class TranslationErrorTransformerImpl implements TranslationErrorTransformer {

    public static final String DEFAULT_ERROR_MESSAGE = "Unexpected translation error. Please contact Chorus support.";
    public static final String UNEXPECTED_ERROR_WHEN_TRANSLATE_FILE_STATUS_PATTERN = ".*?Unexpected error when translate file\\.Status.*?";
    public static final String UNKNOWN_SCAN_TYPE_ITMS_C_PATTERN = ".*?Unknown ScanType ITMS \\+ C.*?";
    public static final String Q3_MS_FILTER_PATTERN = ".*?Q3MS.*?";
    public static final String ESI_FILTER_PATTERN = ".*?Unknown ScanType ITMS \\+ P ESI.*?";
    public static final String MSX_FILTER_PATTERN = ".*?Unknown filter type: FTMS \\+ P NSI FULL MSX.*?";
    public static final String WRONG_FILE_SIZE_PATTERN = ".*?File raw size does not match expected for file.*?";
    public static final String NOT_SUPPORTED_FILE_PATTERN = ".*?InvalidRawFileException: Cannot read functions.*?";

    private static boolean matchMessage(CharSequence message, String regex){
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(message);
        return m.matches();
    }

    @Override
    public String transform(String errorMessage) {
        if(errorMessage == null){
            return null;
        }
        if(!errorMessage.contains("com.infoclinika") && !matchMessage(errorMessage, ".*?Exception.*?:.*?")){
            return errorMessage;
        }

        final String transformedMessage;
        if(matchMessage(errorMessage, UNEXPECTED_ERROR_WHEN_TRANSLATE_FILE_STATUS_PATTERN)){
            transformedMessage = DEFAULT_ERROR_MESSAGE;
        } else if(matchMessage(errorMessage, UNKNOWN_SCAN_TYPE_ITMS_C_PATTERN)){
            transformedMessage = "Centroid MS data type is not supported.";
        } else if(matchMessage(errorMessage, Q3_MS_FILTER_PATTERN)){
            transformedMessage = "Q3MS filter type is not supported.";
        } else if(matchMessage(errorMessage, ESI_FILTER_PATTERN)){
            transformedMessage = "ESI filter type is not supported.";
        } else if(matchMessage(errorMessage, MSX_FILTER_PATTERN)){
            transformedMessage = "MSX filter type is not supported.";
        } else if(matchMessage(errorMessage, WRONG_FILE_SIZE_PATTERN)){
            transformedMessage = "File has not been uploaded fully. Please delete it and re-upload.";
        } else if(matchMessage(errorMessage, NOT_SUPPORTED_FILE_PATTERN)){
            transformedMessage = "File is broken or not supported by our binding. Please delete it and re-upload.";
        } else {
            transformedMessage = DEFAULT_ERROR_MESSAGE;
        }

        return transformedMessage;
    }
}
