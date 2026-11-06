package com.clinic.model;

/**
 * =========================================================================
 * INVARIANTS FOR Patient:
 * 1. Inherits all invariants from Person (id > 0, name non-empty).
 * 2. Role is always defined as "Patient" or its specialized subtype.
 * 3. Discount calculation is polymorphic via calculateCharge().
 * =========================================================================
 *
 * Represents a patient eligible to book appointments at the clinic.
 */
public class Patient extends Person {

    private final boolean emergency;

    /**
     * Standard constructor creating a non-emergency patient.
     *
     * @param id unique positive identifier
     * @param name non-empty patient name
     */
    public Patient(int id, String name) {
        this(id, name, false);
    }

    /**
     * Constructor allowing initial emergency flag assignment.
     *
     * @param id unique positive identifier
     * @param name non-empty patient name
     * @param emergency true if flagged as an emergency patient
     */
    public Patient(int id, String name, boolean emergency) {
        super(id, name);
        this.emergency = emergency;
    }

    @Override
    public String role() {
        return emergency ? "Patient (Emergency)" : "Patient";
    }

    /**
     * Checks if this patient is currently flagged for emergency treatment.
     *
     * @return true if emergency
     */
    public boolean isEmergency() {
        return emergency;
    }

    /**
     * Polymorphic discount multiplier representing patient pricing tier.
     * Standard patient/visitor pays 100% (multiplier = 1.0).
     *
     * @return multiplier between 0.0 and 1.0
     */
    public double discountMultiplier() {
        return 1.0;
    }

    /**
     * Polymorphically computes the final bill for this patient given the staff base fee.
     * Enables Twist 3 (Discounts) without any type inspections.
     *
     * @param baseFee the base consultation fee of the attending staff member
     * @return the discounted fee to be charged to the patient
     */
    public double calculateCharge(double baseFee) {
        return baseFee * discountMultiplier();
    }
}
