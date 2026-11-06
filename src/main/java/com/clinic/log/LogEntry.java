package com.clinic.log;

/** Immutable value object representing a clinic event at a simulated clinic hour. */
public class LogEntry {

    private final int clinicHour;
    private final boolean success;
    private final String message;

    public LogEntry(int clinicHour, boolean success, String message) {
        if (clinicHour < 0 || clinicHour > 168) {
            throw new IllegalArgumentException("Clinic hour must be between 0 and 168.");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Log message must not be blank.");
        }
        this.clinicHour = clinicHour;
        this.success = success;
        this.message = message;
    }

    public int getClinicHour() {
        return clinicHour;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        String statusTag = success ? "SUCCESS" : "FAILED";
        return String.format("[Clinic Hour: %03d] [%s] %s", clinicHour, statusTag, message);
    }
}
