package com.clinic.exception;

/**
 * Base checked/runtime domain exception for the Clinic Appointment System.
 */
public class ClinicException extends RuntimeException {
    public ClinicException(String message) {
        super(message);
    }

    public ClinicException(String message, Throwable cause) {
        super(message, cause);
    }
}
