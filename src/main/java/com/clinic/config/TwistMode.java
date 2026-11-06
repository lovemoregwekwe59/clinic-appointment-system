package com.clinic.config;

/**
 * Enumeration representing the selectable twists in the Clinic Appointment System.
 * In addition to the four student-specific twists (P mod 4), STANDARD mode
 * runs base clinic rules, and ALL_ENABLED enables all twist rules simultaneously.
 */
public enum TwistMode {
    STANDARD("Standard Rules (Base clinic without twist rules)"),
    TWIST_0_WAITING_LIST("Twist 0: Waiting List (Auto-offer cancelled slots to waiting patients)"),
    TWIST_1_EMERGENCY("Twist 1: Emergency (Emergency patients bump PENDING slots)"),
    TWIST_2_BUFFER("Twist 2: Buffer (Doctors need 2h, Nurses 1h, Pharmacists 0h gap)"),
    TWIST_3_DISCOUNTS("Twist 3: Discounts (Students 50%, Staff-members 0%, Visitors 100%)"),
    ALL_ENABLED("All Twists Enabled (Comprehensive combined mode)");

    private final String description;

    TwistMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
