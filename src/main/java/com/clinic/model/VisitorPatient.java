package com.clinic.model;

/**
 * A standard visitor or external patient.
 * Under Twist 3 (Discounts), visitors pay the full consultation fee (100%).
 */
public class VisitorPatient extends Patient {

    public VisitorPatient(int id, String name) {
        super(id, name);
    }

    public VisitorPatient(int id, String name, boolean emergency) {
        super(id, name, emergency);
    }

    @Override
    public String role() {
        return isEmergency() ? "Visitor Patient (Emergency)" : "Visitor Patient";
    }

    @Override
    public double discountMultiplier() {
        return 1.00; // Full fee (100%)
    }
}
