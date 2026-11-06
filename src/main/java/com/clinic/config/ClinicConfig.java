package com.clinic.config;

/**
 * System-wide configuration settings for the Clinic Appointment System.
 * Controls the active twist mode and the university assignment parameter P.
 */
public class ClinicConfig {

    private static volatile int pParameter = 0;
    private static volatile TwistMode activeTwist = TwistMode.TWIST_0_WAITING_LIST;

    private ClinicConfig() {
        // Prevent instantiation of utility configuration class
    }

    /**
     * Returns the university assignment parameter P.
     * Default value is 0.
     *
     * @return current value of P
     */
    public static int getP() {
        return pParameter;
    }

    /**
     * Sets P and selects the corresponding assigned twist using P modulo 4.
     *
     * @param p new value of P (must be non-negative)
     */
    public static void setP(int p) {
        if (p < 0) {
            throw new IllegalArgumentException("Parameter P must be non-negative: " + p);
        }
        pParameter = p;
        activeTwist = twistForParameter(p);
    }

    /**
     * Returns the currently active twist mode.
     *
     * @return current TwistMode
     */
    public static TwistMode getActiveTwist() {
        return activeTwist;
    }

    /**
     * Changes the active twist mode.
     *
     * @param twistMode new active TwistMode
     */
    public static void setActiveTwist(TwistMode twistMode) {
        if (twistMode == null) {
            throw new IllegalArgumentException("TwistMode cannot be null.");
        }
        activeTwist = twistMode;
    }

    private static TwistMode twistForParameter(int p) {
        return switch (p % 4) {
            case 0 -> TwistMode.TWIST_0_WAITING_LIST;
            case 1 -> TwistMode.TWIST_1_EMERGENCY;
            case 2 -> TwistMode.TWIST_2_BUFFER;
            default -> TwistMode.TWIST_3_DISCOUNTS;
        };
    }

    /**
     * Resets configuration to P=0 and its corresponding waiting-list twist.
     */
    public static void resetToDefaults() {
        pParameter = 0;
        activeTwist = twistForParameter(0);
    }
}
