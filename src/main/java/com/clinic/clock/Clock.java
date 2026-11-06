package com.clinic.clock;

/**
 * Clock interface providing the current clinic time.
 * In accordance with clinic requirements, the application must never
 * read the computer's system clock directly. Instead, all time queries
 * must be delegated to an instance of this Clock.
 *
 * Time is represented as a whole number of hours from 0 to 168 (one week),
 * where 0 represents the beginning of the week and 168 represents the end.
 */
public interface Clock {

    /**
     * Returns the current clinic hour, guaranteed to be between 0 and 168.
     *
     * @return current hour (0 to 168)
     */
    int now();
}
