package com.clinic.model;

import com.clinic.config.ClinicConfig;

/**
 * Represents a registered Nurse in the clinic.
 *
 * Rules:
 * - Role: "Nurse"
 * - Fee: 10 + (P mod 3)
 * - Buffer: 1 hour between appointments (Twist 2)
 */
public class Nurse extends Staff {

    private final Integer customP;

    public Nurse(int id, String name) {
        super(id, name);
        this.customP = null;
    }

    public Nurse(int id, String name, int customP) {
        super(id, name);
        this.customP = customP;
    }

    @Override
    public String role() {
        return "Nurse";
    }

    @Override
    public double fee() {
        int p = customP != null ? customP : ClinicConfig.getP();
        return 10.0 + (p % 3);
    }

    @Override
    public int bufferHours() {
        return 1;
    }
}
