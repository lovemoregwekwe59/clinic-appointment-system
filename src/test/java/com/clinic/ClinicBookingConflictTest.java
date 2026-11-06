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

@DisplayName("Clinic Booking Conflict and ID Uniqueness Tests")
class ClinicBookingConflictTest {

    private Clinic clinic;
    private Patient patient1;
    private Patient patient2;
    private Staff doctor;
    private Staff nurse;

    @BeforeEach
    void setUp() {
        // Use STANDARD twist mode to isolate base conflict rules
        clinic = new Clinic(new FixedClock(10), TwistMode.STANDARD);
        patient1 = new VisitorPatient(1, "Alice");
        patient2 = new VisitorPatient(2, "Bob");
        doctor = new Doctor(10, "Dr. Adams");
        nurse = new Nurse(20, "Nurse Baker");

        clinic.registerPatient(patient1);
        clinic.registerPatient(patient2);
        clinic.registerStaff(doctor);
        clinic.registerStaff(nurse);
    }

    @Test
    @DisplayName("IDs must be unique across the entire clinic (Person ID clash)")
    void duplicatePersonIdsRejectedAcrossClinic() {
        // Registering a new patient with an existing patient's ID
        Patient duplicatePatient = new VisitorPatient(1, "Alice Clone");
        ConflictException ex1 = assertThrows(ConflictException.class,
                () -> clinic.registerPatient(duplicatePatient));
        assertTrue(ex1.getMessage().contains("already in use"));

        // Registering a staff member with an existing patient's ID
        Staff staffWithPatientId = new Doctor(1, "Dr. Clash");
        ConflictException ex2 = assertThrows(ConflictException.class,
                () -> clinic.registerStaff(staffWithPatientId));
        assertTrue(ex2.getMessage().contains("already in use"));

        // Registering a patient with an existing staff member's ID
        Patient patientWithStaffId = new VisitorPatient(10, "Patient Clash");
        ConflictException ex3 = assertThrows(ConflictException.class,
                () -> clinic.registerPatient(patientWithStaffId));
        assertTrue(ex3.getMessage().contains("already in use"));
    }

    @Test
    @DisplayName("Appointment IDs are unique with registered person IDs")
    void appointmentIdsDoNotOverlapPersonIds() {
        Appointment appointment = clinic.bookAppointment(patient1.getId(), doctor.getId(), 15);
        assertTrue(appointment.getId() != patient1.getId());
        assertTrue(appointment.getId() != patient2.getId());
        assertTrue(appointment.getId() != doctor.getId());

        Patient collision = new VisitorPatient(appointment.getId(), "Appointment ID collision");
        ConflictException ex = assertThrows(ConflictException.class, () -> clinic.registerPatient(collision));
        assertTrue(ex.getMessage().contains("already in use"));
    }

    @Test
    @DisplayName("Staff member cannot have two non-cancelled appointments at the same hour")
    void staffCannotDoubleBookAtSameHour() {
        // Patient 1 books Doctor at hour 15
        Appointment appt1 = clinic.bookAppointment(patient1.getId(), doctor.getId(), 15);
        assertNotNull(appt1);

        // Patient 2 attempts to book the same Doctor at hour 15 -> must be rejected
        ConflictException ex = assertThrows(ConflictException.class,
                () -> clinic.bookAppointment(patient2.getId(), doctor.getId(), 15));
        assertTrue(ex.getMessage().contains("already booked at hour 15"));
    }

    @Test
    @DisplayName("Patient cannot have two non-cancelled appointments at the same hour")
    void patientCannotDoubleBookAtSameHour() {
        // Patient 1 books Doctor at hour 15
        clinic.bookAppointment(patient1.getId(), doctor.getId(), 15);

        // Patient 1 attempts to book Nurse at the same hour 15 -> must be rejected
        ConflictException ex = assertThrows(ConflictException.class,
                () -> clinic.bookAppointment(patient1.getId(), nurse.getId(), 15));
        assertTrue(ex.getMessage().contains("already has an active appointment at hour 15"));
    }

    @Test
    @DisplayName("Slot can be booked if the previous appointment was cancelled")
    void canBookSlotIfPreviousAppointmentWasCancelled() {
        // Book Doctor at hour 15
        Appointment appt1 = clinic.bookAppointment(patient1.getId(), doctor.getId(), 15);

        // Cancel it (clock is at 10, hour is 15 -> valid window)
        clinic.cancelAppointment(appt1.getId(), "Patient cancelled");
        assertEquals(AppointmentStatus.CANCELLED, appt1.getStatus());

        // Now Patient 2 can successfully book that same slot at hour 15!
        assertDoesNotThrow(() -> clinic.bookAppointment(patient2.getId(), doctor.getId(), 15));
    }
}
