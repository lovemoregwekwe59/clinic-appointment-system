package com.clinic.model;

/**
 * A patient who is an employee or staff member of the university/clinic.
 * Under Twist 3 (Discounts), staff members receive free healthcare (0% charge).
 */
public class StaffPatient extends Patient {

    public StaffPatient(int id, String name) {
        super(id, name);
    }

    public StaffPatient(int id, String name, boolean emergency) {
        super(id, name, emergency);
    }

    @Override
    public String role() {
        return isEmergency() ? "Staff Patient (Emergency)" : "Staff Patient";
    }

    @Override
    public double discountMultiplier() {
        return 0.00; // Free healthcare (0% charge)
    }
}
