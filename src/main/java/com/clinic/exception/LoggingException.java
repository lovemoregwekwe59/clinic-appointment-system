package com.clinic.exception;

/**
 * Thrown when an underlying failure occurs within the AppointmentLog resource.
 */
public class LoggingException extends ClinicException {
    public LoggingException(String message) {
        super(message);
    }

    public LoggingException(String message, Throwable cause) {
        super(message, cause);
    }
}
