package com.clinic.exception;

/**
 * Thrown when an appointment booking conflicts with an existing non-cancelled appointment,
 * a buffer time constraint, or a duplicate identifier.
 */
public class ConflictException extends ClinicException {
    public ConflictException(String message) {
        super(message);
    }
}
