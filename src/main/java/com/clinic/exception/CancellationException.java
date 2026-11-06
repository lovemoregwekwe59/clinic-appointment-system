package com.clinic.exception;

/**
 * Thrown when an appointment cancellation violates clinic timing rules:
 * - Cancelling an appointment that is already cancelled.
 * - Cancelling an appointment that has already started (now >= startHour).
 * - Cancelling a confirmed appointment less than 2 hours before its start.
 */
public class CancellationException extends ClinicException {
    public CancellationException(String message) {
        super(message);
    }
}
