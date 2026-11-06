package com.clinic;

import com.clinic.exception.CancellationException;
import com.clinic.clock.SimulatedClock;
import com.clinic.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Appointment Lifecycle and Timing Rules Tests")
class AppointmentLifecycleTest {

    private Patient patient;
    private Staff staff;
    private SimulatedClock clock;

    @BeforeEach
    void setUp() {
        patient = new VisitorPatient(1, "Alice");
        staff = new Doctor(2, "Dr. House");
        clock = new SimulatedClock(10);
    }

    private Appointment appointment(int id, int startHour) {
        return new Appointment(id, patient, staff, startHour, clock);
    }

    @Test
    @DisplayName("New appointment starts in PENDING status")
    void initialStatusIsPending() {
        Appointment appt = appointment(1, 20);
        assertEquals(AppointmentStatus.PENDING, appt.getStatus());
        assertNull(appt.getCancellationReason());
    }

    @Test
    @DisplayName("PENDING appointment can be confirmed before start time")
    void pendingCanBeConfirmed() {
        Appointment appt = appointment(1, 20);
        appt.confirm();
        assertEquals(AppointmentStatus.CONFIRMED, appt.getStatus());
    }

    @Test
    @DisplayName("Cannot confirm an appointment that has already started or elapsed")
    void cannotConfirmStartedOrElapsedAppointment() {
        Appointment appt = appointment(1, 20);
        clock.setTime(20);
        assertThrows(IllegalStateException.class, appt::confirm);
        clock.setTime(25);
        assertThrows(IllegalStateException.class, appt::confirm);
    }

    @Test
    @DisplayName("PENDING appointment can be cancelled before it starts")
    void pendingCanBeCancelledBeforeStart() {
        Appointment appt = appointment(1, 20);
        clock.setTime(19);
        appt.cancel("Schedule conflict");
        assertEquals(AppointmentStatus.CANCELLED, appt.getStatus());
        assertEquals("Schedule conflict", appt.getCancellationReason());
    }

    @Test
    @DisplayName("CONFIRMED appointment CAN be cancelled >= 2 hours before start")
    void confirmedCanBeCancelledTwoOrMoreHoursBefore() {
        Appointment appt = appointment(1, 20);
        appt.confirm();
        assertEquals(AppointmentStatus.CONFIRMED, appt.getStatus());

        // Cancel at hour 18: start(20) - now(18) = 2 hours -> allowed!
        clock.setTime(18);
        appt.cancel("Feeling better");
        assertEquals(AppointmentStatus.CANCELLED, appt.getStatus());
        assertEquals("Feeling better", appt.getCancellationReason());
    }

    @Test
    @DisplayName("CONFIRMED appointment CANNOT be cancelled less than 2 hours before start")
    void confirmedCannotBeCancelledLessThanTwoHoursBefore() {
        Appointment appt = appointment(1, 20);
        appt.confirm();

        // Cancel at hour 19: start(20) - now(19) = 1 hour (< 2) -> rejected!
        CancellationException ex = assertThrows(CancellationException.class,
                () -> {
                    clock.setTime(19);
                    appt.cancel("Late notice");
                });
        assertTrue(ex.getMessage().contains("less than two hours before start"));
        assertEquals(AppointmentStatus.CONFIRMED, appt.getStatus());
    }

    @Test
    @DisplayName("No appointment can be cancelled once it has started (now >= startHour)")
    void noAppointmentCanBeCancelledOnceStarted() {
        Appointment pendingAppt = appointment(1, 20);
        clock.setTime(20);
        assertThrows(CancellationException.class, () -> pendingAppt.cancel("Started"));
        clock.setTime(21);
        assertThrows(CancellationException.class, () -> pendingAppt.cancel("After start"));

        clock.setTime(10);
        Appointment confirmedAppt = appointment(2, 30);
        confirmedAppt.confirm();
        clock.setTime(30);
        assertThrows(CancellationException.class, () -> confirmedAppt.cancel("Started"));
        clock.setTime(35);
        assertThrows(CancellationException.class, () -> confirmedAppt.cancel("After start"));
    }

    @Test
    @DisplayName("A cancelled appointment can NEVER return to another status")
    void cancelledAppointmentCanNeverReturn() {
        Appointment appt = appointment(1, 20);
        appt.cancel("Changed mind");
        assertEquals(AppointmentStatus.CANCELLED, appt.getStatus());

        // Attempting to confirm a cancelled appointment must fail
        assertThrows(IllegalStateException.class, appt::confirm);

        // Attempting to cancel an already cancelled appointment must fail
        assertThrows(CancellationException.class, () -> appt.cancel("Cancel again"));
        assertEquals(AppointmentStatus.CANCELLED, appt.getStatus());
    }
}
