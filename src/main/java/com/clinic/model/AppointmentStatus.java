package com.clinic.model;

/**
 * Lifecycle states of an appointment.
 * Transitions:
 *   [Initial] -> PENDING
 *   PENDING -> CONFIRMED
 *   PENDING -> CANCELLED
 *   CONFIRMED -> CANCELLED (only allowed if >= 2 hours before start)
 *   CANCELLED -> (Terminal state: cannot transition to any other state)
 */
public enum AppointmentStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
