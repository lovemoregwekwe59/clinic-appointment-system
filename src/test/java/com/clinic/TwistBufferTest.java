package com.clinic;

import com.clinic.clock.FixedClock;
import com.clinic.config.TwistMode;
import com.clinic.exception.ConflictException;
import com.clinic.model.*;
import com.clinic.service.Clinic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Twist 2: Staff Buffer Hours Polymorphism Tests")
class TwistBufferTest {

    private Clinic clinic;
    private Patient patient1;
    private Patient patient2;
    private Staff doctor;
    private Staff nurse;
    private Staff pharmacist;

    @BeforeEach
    void setUp() {
        clinic = new Clinic(new FixedClock(0), TwistMode.TWIST_2_BUFFER);
        patient1 = new VisitorPatient(1, "Alice");
        patient2 = new VisitorPatient(2, "Bob");
        doctor = new Doctor(10, "Dr. Adams");
        nurse = new Nurse(20, "Nurse Baker");
        pharmacist = new Pharmacist(30, "Pharm. Clara");

        clinic.registerPatient(patient1);
        clinic.registerPatient(patient2);
        clinic.registerStaff(doctor);
        clinic.registerStaff(nurse);
        clinic.registerStaff(pharmacist);
    }

    @Test
    @DisplayName("Doctor buffer (2h): appointment at hour 10 forbids 11 and 12, allows 13")
    void doctorRequiresTwoHourBuffer() {
        assertEquals(2, doctor.bufferHours());

        // Book Dr. Adams at hour 10 (10:00 - 11:00)
        clinic.bookAppointment(patient1.getId(), doctor.getId(), 10);

        // Immediate next hour 11 (gap = 0h < 2h) -> rejected
        assertThrows(ConflictException.class,
                () -> clinic.bookAppointment(patient2.getId(), doctor.getId(), 11));

        // Hour 12 (gap = 1h < 2h) -> rejected
        assertThrows(ConflictException.class,
                () -> clinic.bookAppointment(patient2.getId(), doctor.getId(), 12));

        // Hour 13 (gap = 2h >= 2h) -> allowed!
        assertDoesNotThrow(() -> clinic.bookAppointment(patient2.getId(), doctor.getId(), 13));
    }

    @Test
    @DisplayName("Nurse buffer (1h): appointment at hour 10 forbids 11, allows 12")
    void nurseRequiresOneHourBuffer() {
        assertEquals(1, nurse.bufferHours());

        // Book Nurse at hour 10 (10:00 - 11:00)
        clinic.bookAppointment(patient1.getId(), nurse.getId(), 10);

        // Immediate next hour 11 (gap = 0h < 1h) -> rejected
        assertThrows(ConflictException.class,
                () -> clinic.bookAppointment(patient2.getId(), nurse.getId(), 11));

        // Hour 12 (gap = 1h >= 1h) -> allowed!
        assertDoesNotThrow(() -> clinic.bookAppointment(patient2.getId(), nurse.getId(), 12));
    }

    @Test
    @DisplayName("Pharmacist buffer (0h): back-to-back appointments allowed")
    void pharmacistRequiresZeroHourBuffer() {
        assertEquals(0, pharmacist.bufferHours());

        // Book Pharmacist at hour 10 (10:00 - 11:00)
        clinic.bookAppointment(patient1.getId(), pharmacist.getId(), 10);

        // Immediate next hour 11 (gap = 0h >= 0h) -> allowed!
        assertDoesNotThrow(() -> clinic.bookAppointment(patient2.getId(), pharmacist.getId(), 11));
    }
}
