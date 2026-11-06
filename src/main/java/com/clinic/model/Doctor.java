package com.clinic.model;

import com.clinic.config.ClinicConfig;

/**
 * Represents a medical Doctor in the clinic.
 *
 * Rules:
 * - Role: "Doctor"
 * - Fee: 40 + 5 * (P mod 5)
 * - Buffer: 2 hours between appointments (Twist 2)
 */
public class Doctor extends Staff {

    private final Integer customP;

    public Doctor(int id, String name) {
        super(id, name);
        this.customP = null;
    }

    public Doctor(int id, String name, int customP) {
        super(id, name);
        this.customP = customP;
    }

    @Override
    public String role() {
        return "Doctor";
    }

    @Override
    public double fee() {
        int p = customP != null ? customP : ClinicConfig.getP();
        return 40.0 + (5.0 * (p % 5));
    }

    @Override
    public int bufferHours() {
        return 2;
    }
}
