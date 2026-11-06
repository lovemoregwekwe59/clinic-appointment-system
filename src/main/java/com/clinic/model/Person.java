package com.clinic.model;

import com.clinic.exception.ValidationException;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * =========================================================================
 * INVARIANTS FOR Person:
 * 1. ID must be strictly positive (id > 0) and immutable once instantiated.
 * 2. Name must be non-null, non-empty, and non-blank (trimmed length > 0).
 * 3. Encapsulation: Fields are private final, preventing unauthorized alteration.
 * 4. Polymorphic Contract: Subclasses must define their specific role() string.
 * =========================================================================
 *
 * Abstract base class representing any individual registered with the clinic.
 */
public abstract class Person {

    // Counter used to demonstrate that Person constructor is invoked exactly once,
    // especially during multiple inheritance simulation with MedicalLecturer.
    private static final AtomicInteger CONSTRUCTOR_CALL_COUNT = new AtomicInteger(0);

    private final int id;
    private final String name;

    /**
     * Protected constructor enforcing encapsulation and data integrity invariants.
     *
     * @param id unique positive identifier
     * @param name non-empty full name
     * @throws ValidationException if id <= 0 or name is null/empty
     */
    protected Person(int id, String name) {
        if (id <= 0) {
            throw new ValidationException("Person ID must be positive (> 0). Provided: " + id);
        }
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Person name must not be null or blank.");
        }
        this.id = id;
        this.name = name.trim();

        // Increment constructor tracking counter
        CONSTRUCTOR_CALL_COUNT.incrementAndGet();
    }

    /**
     * Unique identifier for this person.
     *
     * @return positive integer ID
     */
    public int getId() {
        return id;
    }

    /**
     * Full name of this person.
     *
     * @return sanitized non-empty name
     */
    public String getName() {
        return name;
    }

    /**
     * Polymorphic method defining the person's clinical or professional role.
     * Must be implemented by concrete subclasses.
     *
     * @return role description string
     */
    public abstract String role();

    /**
     * Returns total times the Person constructor has been called.
     * Used in unit tests and verification.
     *
     * @return constructor call count
     */
    public static int getConstructorCallCount() {
        return CONSTRUCTOR_CALL_COUNT.get();
    }

    /**
     * Resets the constructor call counter (useful before specific test runs).
     */
    public static void resetConstructorCallCount() {
        CONSTRUCTOR_CALL_COUNT.set(0);
    }

    @Override
    public String toString() {
        return String.format("[%s #%d] %s", role(), id, name);
    }
}
