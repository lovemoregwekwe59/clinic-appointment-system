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

@DisplayName("Twist 1: Emergency Bumping Tests")
class TwistEmergencyTest {

    private Clinic clinic;
    private Patient standardPatient;
    private Patient emergencyPatient;
    private Staff doctor;

    @BeforeEach
    void setUp() {
        clinic = new Clinic(new FixedClock(10), TwistMode.TWIST_1_EMERGENCY);
        standardPatient = new VisitorPatient(1, "Standard Sarah", false);
        emergencyPatient = new VisitorPatient(2, "Emergency Eric", true);
        doctor = new Doctor(10, "Dr. Adams");

        clinic.registerPatient(standardPatient);
        clinic.registerPatient(emergencyPatient);
        clinic.registerStaff(doctor);
    }

    @Test
    @DisplayName("Emergency booking bumps PENDING appointment at same hour with reason 'bumped'")
    void emergencyBumpsPendingAppointment() {
        // Sarah books doctor at hour 20 (starts as PENDING)
        Appointment pendingAppt = clinic.bookAppointment(standardPatient.getId(), doctor.getId(), 20);
        assertEquals(AppointmentStatus.PENDING, pendingAppt.getStatus());

        // Emergency patient Eric books doctor at the exact same hour 20
        Appointment emergAppt = clinic.bookAppointment(emergencyPatient.getId(), doctor.getId(), 20);
        assertNotNull(emergAppt);
        assertEquals(emergencyPatient.getId(), emergAppt.getPatient().getId());
        assertEquals(AppointmentStatus.PENDING, emergAppt.getStatus());

        // Sarah's appointment was cancelled with reason "bumped"
        assertEquals(AppointmentStatus.CANCELLED, pendingAppt.getStatus());
        assertEquals("bumped", pendingAppt.getCancellationReason());
    }

    @Test
    @DisplayName("Emergency booking CANNOT bump a CONFIRMED appointment")
    void emergencyCannotBumpConfirmedAppointment() {
        // Sarah books doctor at hour 20 and confirms it
        Appointment confirmedAppt = clinic.bookAppointment(standardPatient.getId(), doctor.getId(), 20);
        clinic.confirmAppointment(confirmedAppt.getId());
        assertEquals(AppointmentStatus.CONFIRMED, confirmedAppt.getStatus());

        // Emergency patient Eric attempts to book the same hour -> must fail!
        ConflictException ex = assertThrows(ConflictException.class,
                () -> clinic.bookAppointment(emergencyPatient.getId(), doctor.getId(), 20));
        assertTrue(ex.getMessage().contains("cannot bump CONFIRMED appointment"));

        // Sarah's appointment remains CONFIRMED
        assertEquals(AppointmentStatus.CONFIRMED, confirmedAppt.getStatus());
    }

    @Test
    @DisplayName("Failed emergency booking does not bump before checking patient conflicts")
    void failedEmergencyBookingLeavesPendingAppointmentUntouched() {
        Appointment pendingAppt = clinic.bookAppointment(standardPatient.getId(), doctor.getId(), 20);
        Staff nurse = new Nurse(11, "Nurse Hall");
        clinic.registerStaff(nurse);
        clinic.bookAppointment(emergencyPatient.getId(), nurse.getId(), 20);

        assertThrows(ConflictException.class,
                () -> clinic.bookAppointment(emergencyPatient.getId(), doctor.getId(), 20));
        assertEquals(AppointmentStatus.PENDING, pendingAppt.getStatus());
        assertNull(pendingAppt.getCancellationReason());
    }

    @Test
    @DisplayName("Non-emergency patient CANNOT bump a PENDING appointment")
    void nonEmergencyCannotBumpPendingAppointment() {
        // Sarah books doctor at hour 20 (PENDING)
        Appointment pendingAppt = clinic.bookAppointment(standardPatient.getId(), doctor.getId(), 20);

        // Another non-emergency patient attempts to book hour 20
        Patient anotherStandard = new VisitorPatient(30, "Bob", false);
        clinic.registerPatient(anotherStandard);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> clinic.bookAppointment(anotherStandard.getId(), doctor.getId(), 20));
        assertTrue(ex.getMessage().contains("already booked at hour 20"));

        assertEquals(AppointmentStatus.PENDING, pendingAppt.getStatus());
    }
}
