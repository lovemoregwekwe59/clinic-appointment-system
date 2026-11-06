package com.clinic.log;

import com.clinic.clock.Clock;
import com.clinic.exception.LoggingException;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Manages an audit logging session for a clinic operation.
 * Implements {@link AutoCloseable} so it can be managed cleanly via Java's
 * try-with-resources construct.
 *
 * Requirements:
 * 1. Open exactly once per clinic operation (prints "[log] opened").
 * 2. Close exactly once even if an error occurs (prints "[log] closed").
 * 3. Record successful and failed actions (e.g. "FAILED cancel #3: too late").
 * 4. Maintain a persistent historical audit trail for review.
 * 5. Handle logging failures carefully (supporting suppressed exceptions under try-with-resources).
 */
public class AppointmentLog implements AutoCloseable {

    private static final List<LogEntry> GLOBAL_HISTORY = new ArrayList<>();
    private static volatile PrintStream outputStream = System.out;
    private static volatile boolean simulateWriteFailure = false;
    private static volatile boolean simulateCloseFailure = false;

    private final Clock clock;
    private final String operationName;
    private final PrintStream sessionOutputStream;
    private boolean closed = false;
    private boolean actionRecorded = false;

    /**
     * Opens an appointment log session for a specific clinic operation.
     * Guaranteed to print "[log] opened".
     *
     * @param clock the clinic clock providing the current hour
     * @param operationName name of the operation being executed
     */
    public AppointmentLog(Clock clock, String operationName) {
        this.clock = Objects.requireNonNull(clock, "Clock must not be null for logging.");
        this.operationName = operationName == null ? "OPERATION" : operationName.trim();
        this.sessionOutputStream = outputStream;
        printLog("[log] opened (" + this.operationName + ")");
    }

    /**
     * Records a successful action in this logging session.
     *
     * @param details message describing the successful action
     */
    public void logSuccess(String details) {
        checkNotClosed();
        LogEntry entry = new LogEntry(clock.now(), true, operationName + ": " + details);
        this.actionRecorded = true;
        writeEntry(entry);
    }

    /**
     * Records a failed action in this logging session.
     * E.g. "FAILED cancel #3: too late".
     *
     * @param reason failure description
     */
    public void logFailure(String reason) {
        checkNotClosed();
        LogEntry entry = new LogEntry(clock.now(), false, "FAILED " + operationName + ": " + reason);
        this.actionRecorded = true;
        writeEntry(entry);
    }

    /**
     * Records a generic info entry.
     *
     * @param message info message
     */
    public void logInfo(String message) {
        checkNotClosed();
        LogEntry entry = new LogEntry(clock.now(), true, operationName + " [INFO]: " + message);
        writeEntry(entry);
    }

    private void writeEntry(LogEntry entry) {
        checkSimulatedWriteFailure();
        printLog("[log] " + entry.getMessage());
        synchronized (GLOBAL_HISTORY) {
            GLOBAL_HISTORY.add(entry);
        }
    }

    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("Cannot write to an AppointmentLog that has already been closed.");
        }
    }

    private void checkSimulatedWriteFailure() {
        if (simulateWriteFailure) {
            throw new LoggingException("Simulated I/O failure while writing to AppointmentLog.");
        }
    }

    /** Closes this session once, attempting the close marker even if close fails. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        RuntimeException failure = null;
        try {
            if (simulateCloseFailure) {
                throw new LoggingException("Simulated I/O failure during AppointmentLog.close().");
            }
        } catch (RuntimeException ex) {
            failure = ex;
        }
        try {
            printLog("[log] closed (" + operationName + ")");
        } catch (RuntimeException ex) {
            if (failure == null) {
                failure = ex;
            } else {
                failure.addSuppressed(ex);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private void printLog(String line) {
        if (sessionOutputStream != null) {
            sessionOutputStream.println(line);
            if (sessionOutputStream.checkError()) {
                throw new LoggingException("Appointment log output failed while writing: " + line);
            }
        }
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean hasActionBeenRecorded() {
        return actionRecorded;
    }

    public String getOperationName() {
        return operationName;
    }

    // --- Static configuration and history access ---

    /**
     * Returns an unmodifiable snapshot of the entire clinic audit history.
     *
     * @return unmodifiable list of all recorded log entries
     */
    public static List<LogEntry> getHistory() {
        synchronized (GLOBAL_HISTORY) {
            return Collections.unmodifiableList(new ArrayList<>(GLOBAL_HISTORY));
        }
    }

    /**
     * Clears all log history (primarily used between unit tests).
     */
    public static void clearHistory() {
        synchronized (GLOBAL_HISTORY) {
            GLOBAL_HISTORY.clear();
        }
    }

    /**
     * Redirects log output to another stream (e.g. for testing output verification).
     *
     * @param stream new print stream, or null to suppress printing
     */
    public static void setOutputStream(PrintStream stream) {
        outputStream = stream;
    }

    public static void setSimulateWriteFailure(boolean simulate) {
        simulateWriteFailure = simulate;
    }

    public static void setSimulateCloseFailure(boolean simulate) {
        simulateCloseFailure = simulate;
    }
}
