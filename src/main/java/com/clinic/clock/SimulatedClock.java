package com.clinic.clock;

/**
 * A mutable simulated clock representing clinic time.
 * Allows moving time forward or setting specific hours during console usage
 * or integration testing, strictly within the range [0, 168].
 */
public class SimulatedClock implements Clock {

    private int currentHour;

    /**
     * Initializes simulated clock at hour 0.
     */
    public SimulatedClock() {
        this(0);
    }

    /**
     * Initializes simulated clock at a specific starting hour.
     *
     * @param initialHour starting hour between 0 and 168
     * @throws IllegalArgumentException if initialHour is not in [0, 168]
     */
    public SimulatedClock(int initialHour) {
        setTime(initialHour);
    }

    @Override
    public int now() {
        return currentHour;
    }

    /**
     * Updates the current time to a specific hour.
     * Time cannot travel backwards in normal simulation.
     *
     * @param hour new hour between 0 and 168
     * @throws IllegalArgumentException if hour is outside [0, 168] or backwards
     */
    public void setTime(int hour) {
        if (hour < 0 || hour > 168) {
            throw new IllegalArgumentException("Clinic time must be between 0 and 168 hours. Provided: " + hour);
        }
        this.currentHour = hour;
    }

    /**
     * Advances time by the given positive number of hours.
     *
     * @param hours hours to advance
     * @throws IllegalArgumentException if hours <= 0 or result exceeds 168
     */
    public void advance(int hours) {
        if (hours < 0) {
            throw new IllegalArgumentException("Cannot advance clock by negative hours: " + hours);
        }
        int newHour = this.currentHour + hours;
        if (newHour > 168) {
            throw new IllegalArgumentException("Cannot advance beyond the 168-hour week. Resulting hour: " + newHour);
        }
        this.currentHour = newHour;
    }

    @Override
    public String toString() {
        return "SimulatedClock{now=" + currentHour + "/168}";
    }
}
