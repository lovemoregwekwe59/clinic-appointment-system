package com.clinic.exception;

/**
 * Thrown when entity input parameters (such as name, ID, or hour) fail validation checks.
 */
public class ValidationException extends ClinicException {
    public ValidationException(String message) {
        super(message);
    }
}
