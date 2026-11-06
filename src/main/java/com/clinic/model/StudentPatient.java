package com.clinic.model;

/**
 * A patient who is a university student.
 * Under Twist 3 (Discounts), students receive a 50% discount on staff consultation fees.
 */
public class StudentPatient extends Patient {

    public StudentPatient(int id, String name) {
        super(id, name);
    }

    public StudentPatient(int id, String name, boolean emergency) {
        super(id, name, emergency);
    }

    @Override
    public String role() {
        return isEmergency() ? "Student Patient (Emergency)" : "Student Patient";
    }

    @Override
    public double discountMultiplier() {
        return 0.50; // 50% discount
    }
}
