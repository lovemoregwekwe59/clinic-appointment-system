package com.clinic.clock;

/**
 * An immutable fixed clock that always returns a predetermined hour.
 * Ideal for deterministic unit tests.
 */
public class FixedClock implements Clock {

    private final int fixedHour;

    /**
     * Creates a fixed clock at the specified hour.
     *
     * @param fixedHour constant hour between 0 and 168
     * @throws IllegalArgumentException if hour is not within [0, 168]
     */
    public FixedClock(int fixedHour) {
        if (fixedHour < 0 || fixedHour > 168) {
            throw new IllegalArgumentException("Clinic time must be between 0 and 168 hours. Provided: " + fixedHour);
        }
        this.fixedHour = fixedHour;
    }

    @Override
    public int now() {
        return fixedHour;
    }

    @Override
    public String toString() {
        return "FixedClock{hour=" + fixedHour + "}";
    }
}
