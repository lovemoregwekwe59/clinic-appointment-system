package com.clinic;

import com.clinic.exception.ValidationException;
import com.clinic.model.*;
import com.clinic.service.Clinic;
import com.clinic.clock.FixedClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Validation and Encapsulation Invariants Tests")
class ValidationAndInvariantsTest {

    @Test
    @DisplayName("Person constructor must reject non-positive IDs")
    void personRejectsNonPositiveIds() {
        assertThrows(ValidationException.class, () -> new Patient(0, "Alice"));
        assertThrows(ValidationException.class, () -> new Patient(-5, "Alice"));
        assertThrows(ValidationException.class, () -> new Doctor(0, "Dr. Smith"));
        assertThrows(ValidationException.class, () -> new Doctor(-1, "Dr. Smith"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    @DisplayName("Person constructor must reject empty or blank names")
    void personRejectsEmptyOrBlankNames(String invalidName) {
        assertThrows(ValidationException.class, () -> new Patient(1, invalidName));
        assertThrows(ValidationException.class, () -> new Doctor(2, invalidName));
        assertThrows(ValidationException.class, () -> new Nurse(3, invalidName));
        assertThrows(ValidationException.class, () -> new Pharmacist(4, invalidName));
    }

    @Test
    @DisplayName("Person constructor must reject null name")
    void personRejectsNullName() {
        assertThrows(ValidationException.class, () -> new Patient(1, null));
        assertThrows(ValidationException.class, () -> new Doctor(2, null));
    }

    @Test
    @DisplayName("Appointment constructor rejects non-positive ID")
    void appointmentRejectsNonPositiveId() {
        Patient p = new Patient(1, "Alice");
        Doctor d = new Doctor(2, "Dr. Smith");
        assertThrows(ValidationException.class, () -> new Appointment(0, p, d, 10, new FixedClock(10)));
        assertThrows(ValidationException.class, () -> new Appointment(-10, p, d, 10, new FixedClock(10)));
    }

    @Test
    @DisplayName("Appointment constructor rejects null patient or staff")
    void appointmentRejectsNullEntities() {
        Patient p = new Patient(1, "Alice");
        Doctor d = new Doctor(2, "Dr. Smith");
        assertThrows(ValidationException.class, () -> new Appointment(10, null, d, 10, new FixedClock(10)));
        assertThrows(ValidationException.class, () -> new Appointment(10, p, null, 10, new FixedClock(10)));
    }

    @Test
    @DisplayName("Appointment constructor rejects hours outside [0, 167]")
    void appointmentRejectsOutOfRangeHours() {
        Patient p = new Patient(1, "Alice");
        Doctor d = new Doctor(2, "Dr. Smith");
        assertThrows(ValidationException.class, () -> new Appointment(10, p, d, -1, new FixedClock(10)));
        assertThrows(ValidationException.class, () -> new Appointment(10, p, d, 168, new FixedClock(10)));
        assertThrows(ValidationException.class, () -> new Appointment(10, p, d, 200, new FixedClock(10)));
        assertDoesNotThrow(() -> new Appointment(10, p, d, 0, new FixedClock(10)));
        assertDoesNotThrow(() -> new Appointment(10, p, d, 167, new FixedClock(10)));
    }

    @Test
    @DisplayName("MedicalLecturer rejects null or empty module name")
    void medicalLecturerRejectsInvalidModuleName() {
        assertThrows(ValidationException.class, () -> new MedicalLecturer(1, "Dr. Jones", null));
        assertThrows(ValidationException.class, () -> new MedicalLecturer(1, "Dr. Jones", "   "));
        assertDoesNotThrow(() -> new MedicalLecturer(1, "Dr. Jones", "Virology"));
    }

    @Test
    @DisplayName("Clinic encapsulation: returned lists must be unmodifiable")
    void clinicListsAreUnmodifiable() {
        Clinic clinic = new Clinic(new FixedClock(10));
        Patient p = new Patient(1, "Alice");
        Staff s = new Doctor(2, "Dr. Smith");
        clinic.registerPatient(p);
        clinic.registerStaff(s);

        assertThrows(UnsupportedOperationException.class, () -> clinic.getPatients().add(new Patient(3, "Bob")));
        assertThrows(UnsupportedOperationException.class, () -> clinic.getStaffMembers().add(new Nurse(4, "Joy")));
        assertThrows(UnsupportedOperationException.class,
                () -> clinic.getAppointments().add(new Appointment(5, p, s, 12, new FixedClock(10))));
    }
}
