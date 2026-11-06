package com.clinic.model;

/**
 * Represents a Pharmacist in the clinic.
 *
 * Rules:
 * - Role: "Pharmacist"
 * - Fee: 5
 * - Buffer: 0 hours between appointments (Twist 2)
 */
public class Pharmacist extends Staff {

    public Pharmacist(int id, String name) {
        super(id, name);
    }

    @Override
    public String role() {
        return "Pharmacist";
    }

    @Override
    public double fee() {
        return 5.0;
    }

    @Override
    public int bufferHours() {
        return 0;
    }
}
