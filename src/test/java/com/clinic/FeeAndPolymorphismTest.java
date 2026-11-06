package com.clinic;

import com.clinic.clock.FixedClock;
import com.clinic.config.ClinicConfig;
import com.clinic.config.TwistMode;
import com.clinic.model.*;
import com.clinic.service.Clinic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Staff Fees and Polymorphism Tests (Assignment Question 3)")
class FeeAndPolymorphismTest {

    @AfterEach
    void tearDown() {
        ClinicConfig.resetToDefaults();
    }

    @Test
    @DisplayName("Fee calculations match formulas: Doctor=40+5*(P%5), Nurse=10+(P%3), Pharmacist=5")
    void feeCalculationsMatchFormula() {
        // Test with P = 0
        ClinicConfig.setP(0);
        Doctor d0 = new Doctor(1, "Dr. Zero");
        Nurse n0 = new Nurse(2, "Nurse Zero");
        Pharmacist p0 = new Pharmacist(3, "Pharm. Zero");

        assertEquals(40.0, d0.fee(), 0.001); // 40 + 5*(0%5) = 40
        assertEquals(10.0, n0.fee(), 0.001); // 10 + (0%3) = 10
        assertEquals(5.0, p0.fee(), 0.001);  // constant 5

        // Test with P = 7:
        // Doctor: 40 + 5*(7%5) = 40 + 5*2 = 50
        // Nurse: 10 + (7%3) = 10 + 1 = 11
        ClinicConfig.setP(7);
        Doctor d7 = new Doctor(4, "Dr. Seven");
        Nurse n7 = new Nurse(5, "Nurse Seven");
        Pharmacist p7 = new Pharmacist(6, "Pharm. Seven");

        assertEquals(50.0, d7.fee(), 0.001);
        assertEquals(11.0, n7.fee(), 0.001);
        assertEquals(5.0, p7.fee(), 0.001);
    }

    @Test
    @DisplayName("P modulo four selects the matching clinic twist")
    void parameterSelectsAssignedTwist() {
        ClinicConfig.setP(4);
        assertEquals(TwistMode.TWIST_0_WAITING_LIST, ClinicConfig.getActiveTwist());
        ClinicConfig.setP(5);
        assertEquals(TwistMode.TWIST_1_EMERGENCY, ClinicConfig.getActiveTwist());
        ClinicConfig.setP(6);
        assertEquals(TwistMode.TWIST_2_BUFFER, ClinicConfig.getActiveTwist());
        ClinicConfig.setP(7);
        assertEquals(TwistMode.TWIST_3_DISCOUNTS, ClinicConfig.getActiveTwist());
    }

    @Test
    @DisplayName("Polymorphic loop prints staff roles and fees")
    void polymorphicFeeLoopPrintsCorrectly() {
        Clinic clinic = new Clinic(new FixedClock(0));
        clinic.registerStaff(new Doctor(1, "Dr. House"));
        clinic.registerStaff(new Nurse(2, "Nurse Joy"));
        clinic.registerStaff(new Pharmacist(3, "Pharm. Bob"));
        clinic.registerStaff(new MedicalLecturer(4, "Prof. Watson", "Pathology"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        clinic.printStaffRolesAndFees(new PrintStream(baos));
        String output = baos.toString();

        assertTrue(output.contains("Doctor"));
        assertTrue(output.contains("Nurse"));
        assertTrue(output.contains("Pharmacist"));
        assertTrue(output.contains("Doctor and Lecturer"));
    }

    @Test
    @DisplayName("Extensibility (OCP): Adding a new Staff subclass requires zero changes to Clinic or loop")
    void addNewStaffSubclassWithoutModifyingClinic() {
        // Define a brand new staff subclass on the fly
        class Physiotherapist extends Staff {
            public Physiotherapist(int id, String name) {
                super(id, name);
            }
            @Override
            public String role() {
                return "Physiotherapist";
            }
            @Override
            public double fee() {
                return 25.0;
            }
            @Override
            public int bufferHours() {
                return 1;
            }
        }

        Clinic clinic = new Clinic(new FixedClock(0));
        Physiotherapist physio = new Physiotherapist(99, "Helen Stone");

        // Clinic registers and manages the new staff subclass seamlessly
        assertDoesNotThrow(() -> clinic.registerStaff(physio));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        clinic.printStaffRolesAndFees(new PrintStream(baos));
        String output = baos.toString();

        assertTrue(output.contains("Physiotherapist"));
        assertTrue(output.contains("25.00"));
    }
}
