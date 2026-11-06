package com.clinic;

import com.clinic.clock.FixedClock;
import com.clinic.exception.CancellationException;
import com.clinic.exception.LoggingException;
import com.clinic.log.AppointmentLog;
import com.clinic.log.LogEntry;
import com.clinic.model.Appointment;
import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.model.VisitorPatient;
import com.clinic.service.Clinic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AppointmentLog and Resource Management Tests (Assignment Question 4)")
class LoggingTest {

    private ByteArrayOutputStream logOut;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        AppointmentLog.clearHistory();
        logOut = new ByteArrayOutputStream();
        AppointmentLog.setOutputStream(new PrintStream(logOut));
    }

    @AfterEach
    void tearDown() {
        AppointmentLog.setOutputStream(System.out);
        AppointmentLog.setSimulateWriteFailure(false);
        AppointmentLog.setSimulateCloseFailure(false);
    }

    @Test
    @DisplayName("Log prints [log] opened and [log] closed exactly once on successful operation")
    void logOpensAndClosesExactlyOnceOnSuccess() {
        FixedClock clock = new FixedClock(10);
        Clinic clinic = new Clinic(clock);

        clinic.registerPatient(new VisitorPatient(1, "Alice"));

        String output = logOut.toString();
        assertTrue(output.contains("[log] opened (register patient #1)"));
        assertTrue(output.contains("[log] closed (register patient #1)"));

        // Verify counts
        int openCount = countOccurrences(output, "[log] opened");
        int closeCount = countOccurrences(output, "[log] closed");
        assertEquals(1, openCount);
        assertEquals(1, closeCount);
    }

    @Test
    @DisplayName("Log prints [log] opened, records FAILED message, and prints [log] closed even on failure")
    void logOpensAndClosesExactlyOnceOnFailure() {
        FixedClock clock = new FixedClock(14); // Clock is at hour 14
        Clinic clinic = new Clinic(clock);

        Patient patient = new VisitorPatient(1, "Alice");
        Doctor doctor = new Doctor(2, "Dr. House");
        clinic.registerPatient(patient);
        clinic.registerStaff(doctor);

        Appointment appt = clinic.bookAppointment(patient.getId(), doctor.getId(), 15);
        clinic.confirmAppointment(appt.getId());

        logOut.reset(); // Clear setup logs

        // Attempt invalid cancellation: 1 hour before start hour 15 (clock is 14)
        assertThrows(CancellationException.class, () -> clinic.cancelAppointment(appt.getId(), "too late"));

        String output = logOut.toString();
        assertTrue(output.contains("[log] opened (cancel #" + appt.getId() + ")"));
        assertTrue(output.contains("[log] FAILED cancel #" + appt.getId() + ": A confirmed appointment cannot be cancelled less than two hours before start"));
        assertTrue(output.contains("[log] closed (cancel #" + appt.getId() + ")"));

        // Audit trail also contains the failed entry
        List<LogEntry> history = AppointmentLog.getHistory();
        boolean foundFailedEntry = history.stream()
                .anyMatch(e -> !e.isSuccess() && e.getMessage().contains("FAILED cancel #" + appt.getId()));
        assertTrue(foundFailedEntry);
    }

    @Test
    @DisplayName("Writing to closed AppointmentLog throws IllegalStateException")
    void cannotWriteToClosedLog() {
        FixedClock clock = new FixedClock(10);
        AppointmentLog log = new AppointmentLog(clock, "testOp");
        log.close();
        assertTrue(log.isClosed());
        assertThrows(IllegalStateException.class, () -> log.logSuccess("should fail"));
    }

    @Test
    @DisplayName("Write failures abort before clinic state changes and still close the log")
    void writeFailureDoesNotCommitRegistration() {
        Clinic clinic = new Clinic(new FixedClock(10));
        AppointmentLog.setSimulateWriteFailure(true);

        LoggingException failure = assertThrows(LoggingException.class,
                () -> clinic.registerPatient(new VisitorPatient(1, "Alice")));

        assertTrue(clinic.getPatients().isEmpty());
        assertTrue(failure.getSuppressed().length > 0);
        assertTrue(logOut.toString().contains("[log] opened (register patient #1)"));
        assertTrue(logOut.toString().contains("[log] closed (register patient #1)"));
    }

    @Test
    @DisplayName("Close failures are surfaced after the action, with the close marker attempted once")
    void closeFailureIsSurfaced() {
        Clinic clinic = new Clinic(new FixedClock(10));
        AppointmentLog.setSimulateCloseFailure(true);

        assertThrows(LoggingException.class,
                () -> clinic.registerPatient(new VisitorPatient(1, "Alice")));

        assertEquals(1, clinic.getPatients().size());
        assertEquals(1, countOccurrences(logOut.toString(), "[log] opened"));
        assertEquals(1, countOccurrences(logOut.toString(), "[log] closed"));
    }

    private int countOccurrences(String text, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
