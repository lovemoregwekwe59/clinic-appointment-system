package com.clinic;

import com.clinic.config.ClinicConfig;
import com.clinic.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Multiple Inheritance and MedicalLecturer Tests (Assignment Question 5)")
class MedicalLecturerMultipleInheritanceTest {

    @BeforeEach
    void setUp() {
        Person.resetConstructorCallCount();
        ClinicConfig.resetToDefaults();
    }

    @Test
    @DisplayName("MedicalLecturer.role() returns exactly 'Doctor and Lecturer'")
    void roleReturnsDoctorAndLecturer() {
        MedicalLecturer ml = new MedicalLecturer(1, "Dr. John Watson", "Forensics");
        assertEquals("Doctor and Lecturer", ml.role());
    }

    @Test
    @DisplayName("MedicalLecturer exposes its doctor and lecturer contracts")
    void exposesDoctorAndLecturerContracts() {
        MedicalLecturer ml = new MedicalLecturer(1, "Dr. John Watson", "Forensics");
        Lecturer lecturer = ml;
        Doctor doctor = ml;
        Staff staff = ml;
        Person person = ml;
        assertEquals("Forensics", lecturer.getModuleName());
        assertEquals("Doctor and Lecturer", doctor.role());
        assertEquals(doctor.fee(), staff.fee());
        assertEquals(1, person.getId());
    }

    @Test
    @DisplayName("Person constructor runs exactly ONCE when MedicalLecturer is instantiated")
    void personConstructorRunsExactlyOnce() {
        assertEquals(0, Person.getConstructorCallCount(), "Counter must be 0 initially");

        // Instantiate MedicalLecturer
        MedicalLecturer ml = new MedicalLecturer(50, "Prof. Gregory House", "Diagnostics");

        // In Java's single inheritance hierarchy:
        // MedicalLecturer -> Doctor -> Staff -> Person
        // The constructor chain runs linearly, invoking Person constructor exactly 1 time.
        assertEquals(1, Person.getConstructorCallCount(),
                "Person's constructor must run exactly ONCE per MedicalLecturer instantiation");
    }

    @Test
    @DisplayName("MedicalLecturer.fee() returns standard Doctor fee (super.fee())")
    void medicalLecturerFeeEqualsDoctorFee() {
        ClinicConfig.setP(3); // P = 3 -> Doctor fee = 40 + 5*(3%5) = 55
        MedicalLecturer ml = new MedicalLecturer(1, "Dr. John", "Surgery");
        Doctor doctor = new Doctor(2, "Dr. Standard");

        assertEquals(55.0, ml.fee(), 0.001);
        assertEquals(doctor.fee(), ml.fee(), 0.001);
    }
}
