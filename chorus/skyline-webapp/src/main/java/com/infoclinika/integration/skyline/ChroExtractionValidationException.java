package com.infoclinika.integration.skyline;

import com.infoclinika.mssharing.model.extraction.exception.ChroExtractionException;

/**
 * @author Oleksii Tymchenko
 */
public class ChroExtractionValidationException extends ChroExtractionException {
    public ChroExtractionValidationException(String message) {
        super(message);
    }
}
