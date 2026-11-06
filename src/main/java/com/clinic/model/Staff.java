package com.clinic.model;

/**
 * =========================================================================
 * INVARIANTS FOR Staff:
 * 1. Inherits all invariants from Person (id > 0, name non-empty).
 * 2. Abstract fee calculation: every concrete staff member must provide a
 *    polymorphic fee() method representing their consultation rate.
 * 3. Abstract buffer requirement: every concrete staff member must provide
 *    bufferHours() indicating the required gap between consecutive appointments.
 * 4. Extensibility (OCP): New healthcare provider roles can be introduced
 *    without modifying appointment loops or clinic business logic.
 * =========================================================================
 *
 * Abstract base class for all clinic healthcare practitioners.
 */
public abstract class Staff extends Person {

    /**
     * Protected constructor initializing staff member identity.
     *
     * @param id unique positive identifier
     * @param name non-empty staff name
     */
    protected Staff(int id, String name) {
        super(id, name);
    }

    /**
     * Polymorphic consultation fee charged for a one-hour appointment.
     *
     * @return fee in currency units
     */
    public abstract double fee();

    /**
     * Polymorphic buffer requirement: specifies how many idle hours
     * this staff member requires before and after an appointment.
     * Doctors require 2 hours, Nurses 1 hour, Pharmacists 0 hours.
     *
     * @return required buffer in whole hours
     */
    public abstract int bufferHours();
}
