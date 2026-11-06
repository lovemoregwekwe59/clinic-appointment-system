package com.clinic.service;

import com.clinic.clock.Clock;
import com.clinic.config.ClinicConfig;
import com.clinic.config.TwistMode;
import com.clinic.exception.ClinicException;
import com.clinic.exception.ConflictException;
import com.clinic.exception.ValidationException;
import com.clinic.log.AppointmentLog;
import com.clinic.log.LogEntry;
import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.Patient;
import com.clinic.model.Person;
import com.clinic.model.Staff;

import java.io.PrintStream;
import java.util.*;

/**
 * =========================================================================
 * CLASS INVARIANTS FOR Clinic:
 * 1. Global Person ID Uniqueness: Every registered Person (Patient or Staff)
 *    must possess a strictly positive, unique integer ID across the clinic.
 * 2. Global ID Uniqueness: Person and appointment IDs are positive and unique
 *    across every registered clinic object; cancelled appointments retain IDs.
 * 3. Schedule Bounds: All appointments must have 0 <= startHour < 168.
 * 4. Single Booking Constraints (Standard):
 *    - A staff member cannot have more than one non-cancelled appointment at the same hour.
 *    - A patient cannot have more than one non-cancelled appointment at the same hour.
 * 5. Clock Isolation: Clinic never queries the host operating system clock.
 *    All time queries strictly call clock.now().
 * 6. Encapsulation: Internal data structures (maps, lists, waiting queues)
 *    are strictly private. Accessors return unmodifiable views (Collections.unmodifiableList).
 * 7. Logging Contract: Every clinic state-mutating operation runs inside an
 *    AppointmentLog try-with-resources block, ensuring exactly one open and
 *    one close event, even in the event of an exception.
 * 8. Open-Closed Principle: Staff fees, roles, and buffer constraints are
 *    polymorphically dispatched without runtime type inspection.
 * =========================================================================
 *
 * Core service coordinating clinic staff, patients, appointments, waiting lists,
 * and business rules.
 */
public class Clinic {

    private final Clock clock;
    private final Map<Integer, Person> personsById;
    private final Map<Integer, Patient> patientsById;
    private final Map<Integer, Staff> staffById;
    private final Map<Integer, Appointment> appointmentsById;
    private final List<Appointment> appointmentList;
    private final Map<Integer, List<Patient>> staffWaitingLists; // Staff ID -> Waiting Patients (Twist 0)

    private long nextAppointmentId = 1;
    private TwistMode twistMode;

    /**
     * Constructs a Clinic with the twist currently selected by ClinicConfig.
     *
     * @param clock clock implementation (SimulatedClock or FixedClock)
     */
    public Clinic(Clock clock) {
        this(clock, ClinicConfig.getActiveTwist());
    }

    /**
     * Constructs a Clinic with a specified Clock and explicit TwistMode.
     *
     * @param clock clock implementation
     * @param twistMode initial active twist configuration
     */
    public Clinic(Clock clock, TwistMode twistMode) {
        this.clock = Objects.requireNonNull(clock, "Clock must not be null.");
        this.twistMode = Objects.requireNonNull(twistMode, "TwistMode must not be null.");
        this.personsById = new LinkedHashMap<>();
        this.patientsById = new LinkedHashMap<>();
        this.staffById = new LinkedHashMap<>();
        this.appointmentsById = new LinkedHashMap<>();
        this.appointmentList = new ArrayList<>();
        this.staffWaitingLists = new LinkedHashMap<>();
    }

    // =========================================================================
    // REGISTRATION (Patients and Staff)
    // =========================================================================

    /**
     * Registers a new patient.
     * Enforces unique ID across all persons in the clinic.
     *
     * @param patient patient to register
     * @throws ValidationException if patient is null or ID is duplicate
     */
    public synchronized void registerPatient(Patient patient) {
        String operation = patient == null ? "register patient" : "register patient #" + patient.getId();
        try (AppointmentLog log = new AppointmentLog(clock, operation)) {
            try {
                if (patient == null) {
                    throw new ValidationException("Cannot register null patient.");
                }
                checkUniquePersonId(patient.getId());
                log.logSuccess("Registered " + patient.role() + " '" + patient.getName() + "' with ID #" + patient.getId());
                personsById.put(patient.getId(), patient);
                patientsById.put(patient.getId(), patient);
            } catch (RuntimeException ex) {
                recordFailure(log, ex);
                throw ex;
            }
        }
    }

    /**
     * Registers a new staff member.
     * Enforces unique ID across all persons in the clinic.
     *
     * @param staff staff member to register
     * @throws ValidationException if staff is null or ID is duplicate
     */
    public synchronized void registerStaff(Staff staff) {
        String operation = staff == null ? "register staff" : "register staff #" + staff.getId();
        try (AppointmentLog log = new AppointmentLog(clock, operation)) {
            try {
                if (staff == null) {
                    throw new ValidationException("Cannot register null staff member.");
                }
                checkUniquePersonId(staff.getId());
                log.logSuccess("Registered " + staff.role() + " '" + staff.getName() + "' with ID #" + staff.getId());
                personsById.put(staff.getId(), staff);
                staffById.put(staff.getId(), staff);
                staffWaitingLists.put(staff.getId(), new ArrayList<>());
            } catch (RuntimeException ex) {
                recordFailure(log, ex);
                throw ex;
            }
        }
    }

    private void checkUniquePersonId(int id) {
        if (isIdInUse(id)) {
            throw new ConflictException("ID #" + id + " is already in use. IDs must be unique across the entire clinic.");
        }
    }

    private boolean isIdInUse(int id) {
        return personsById.containsKey(id) || appointmentsById.containsKey(id);
    }

    private int nextAvailableAppointmentId() {
        while (nextAppointmentId <= Integer.MAX_VALUE && isIdInUse((int) nextAppointmentId)) {
            nextAppointmentId++;
        }
        if (nextAppointmentId > Integer.MAX_VALUE) {
            throw new ConflictException("No positive appointment IDs remain.");
        }
        return (int) nextAppointmentId++;
    }

    private int currentHour() {
        int now = clock.now();
        if (now < 0 || now > 168) {
            throw new IllegalStateException("Clock.now() must return an hour between 0 and 168; got " + now + ".");
        }
        return now;
    }

    private void recordFailure(AppointmentLog log, RuntimeException operationFailure) {
        try {
            log.logFailure(operationFailure.getMessage() == null
                    ? operationFailure.getClass().getSimpleName()
                    : operationFailure.getMessage());
        } catch (RuntimeException loggingFailure) {
            operationFailure.addSuppressed(loggingFailure);
        }
    }

    // =========================================================================
    // APPOINTMENT BOOKING & SCHEDULING
    // =========================================================================

    /**
     * Books a 1-hour appointment for a patient with a staff member at startHour.
     *
     * @param patientId registered patient ID
     * @param staffId registered staff ID
     * @param startHour hour between 0 and 167
     * @return the created Appointment
     */
    public synchronized Appointment bookAppointment(int patientId, int staffId, int startHour) {
        String opName = "book patient #" + patientId + " with staff #" + staffId + " at hour " + startHour;
        try (AppointmentLog log = new AppointmentLog(clock, opName)) {
            try {
                Patient patient = getExistingPatient(patientId);
                Staff staff = getExistingStaff(staffId);

                validateAppointmentHour(startHour);

                // Check for Emergency Twist (Twist 1)
                boolean isEmergency = patient.isEmergency() && isTwistActive(TwistMode.TWIST_1_EMERGENCY);

                // Check staff schedule conflicts
                Appointment conflictingStaffAppt = findNonCancelledAppointmentForStaffAt(staffId, startHour);
                if (conflictingStaffAppt != null) {
                    if (isEmergency) {
                        if (conflictingStaffAppt.getStatus() == AppointmentStatus.CONFIRMED) {
                            throw new ConflictException("Emergency booking cannot bump CONFIRMED appointment #" + conflictingStaffAppt.getId());
                        } else if (conflictingStaffAppt.getStatus() == AppointmentStatus.PENDING) {
                            conflictingStaffAppt.validateCancellation();
                        }
                    } else {
                        throw new ConflictException("Staff " + staff.getName() + " (#" + staffId + ") is already booked at hour " + startHour + " (Appointment #" + conflictingStaffAppt.getId() + ").");
                    }
                }

                // Check patient schedule conflicts
                Appointment conflictingPatientAppt = findNonCancelledAppointmentForPatientAt(patientId, startHour);
                if (conflictingPatientAppt != null) {
                    throw new ConflictException("Patient " + patient.getName() + " (#" + patientId + ") already has an active appointment at hour " + startHour + " (Appointment #" + conflictingPatientAppt.getId() + ").");
                }

                // Check Twist 2: Buffer constraints
                if (isTwistActive(TwistMode.TWIST_2_BUFFER)) {
                    checkStaffBufferConstraint(staff, startHour, conflictingStaffAppt);
                }

                int appointmentId = nextAvailableAppointmentId();
                Appointment appointment = new Appointment(appointmentId, patient, staff, startHour, clock,
                        isTwistActive(TwistMode.TWIST_3_DISCOUNTS));

                String msg = "Booked appointment #" + appointmentId + " for " + patient.getName()
                        + " with " + staff.role() + " " + staff.getName() + " at hour " + startHour + "-" + (startHour + 1);
                if (conflictingStaffAppt != null && isEmergency) {
                    msg += " (PENDING appointment #" + conflictingStaffAppt.getId() + " bumped; reason: bumped)";
                }
                log.logSuccess(msg);
                if (conflictingStaffAppt != null && isEmergency) {
                    conflictingStaffAppt.cancel("bumped");
                }
                appointmentsById.put(appointmentId, appointment);
                appointmentList.add(appointment);
                return appointment;

            } catch (RuntimeException ex) {
                recordFailure(log, ex);
                throw ex;
            }
        }
    }

    /**
     * Checks if staff member has sufficient buffer around the target hour (Twist 2).
     */
    private void checkStaffBufferConstraint(Staff staff, int targetHour, Appointment ignoredAppointment) {
        int buffer = staff.bufferHours();
        if (buffer <= 0) {
            return;
        }

        for (Appointment appt : appointmentList) {
            if (appt != ignoredAppointment && appt.getStaff().getId() == staff.getId()
                    && appt.getStatus() != AppointmentStatus.CANCELLED) {
                int existingStart = appt.getStartHour();
                int diff = Math.abs(targetHour - existingStart);
                if (diff < (1 + buffer)) {
                    throw new ConflictException(String.format(
                            "Buffer constraint violated for %s %s: requires %d-hour gap between appointments. Existing appointment #%d at hour %d conflicts with target hour %d.",
                            staff.role(), staff.getName(), buffer, appt.getId(), existingStart, targetHour));
                }
            }
        }
    }

    // =========================================================================
    // APPOINTMENT CONFIRMATION & CANCELLATION
    // =========================================================================

    /**
     * Confirms an appointment.
     *
     * @param appointmentId ID of appointment to confirm
     */
    public synchronized void confirmAppointment(int appointmentId) {
        String opName = "confirm #" + appointmentId;
        try (AppointmentLog log = new AppointmentLog(clock, opName)) {
            try {
                Appointment appointment = getExistingAppointment(appointmentId);
                appointment.validateConfirmation();
                int now = currentHour();
                log.logSuccess("Appointment #" + appointmentId + " confirmed at clinic hour " + now);
                appointment.confirm();
            } catch (RuntimeException ex) {
                recordFailure(log, ex);
                throw ex;
            }
        }
    }

    /**
     * Cancels an appointment.
     * If Twist 0 (Waiting List) is enabled, automatically offers the newly opened
     * slot to the first waiting patient with no scheduling conflict.
     *
     * @param appointmentId ID of appointment to cancel
     * @param reason non-empty reason for cancellation
     */
    public synchronized void cancelAppointment(int appointmentId, String reason) {
        String opName = "cancel #" + appointmentId;
        try (AppointmentLog log = new AppointmentLog(clock, opName)) {
            try {
                Appointment appointment = getExistingAppointment(appointmentId);
                appointment.validateCancellation();
                Patient waitingPatient = null;
                int newAppointmentId = -1;
                Appointment waitingAppointment = null;
                if (isTwistActive(TwistMode.TWIST_0_WAITING_LIST)) {
                    waitingPatient = findWaitingPatientForSlot(appointment);
                    if (waitingPatient != null) {
                        newAppointmentId = nextAvailableAppointmentId();
                        waitingAppointment = new Appointment(newAppointmentId, waitingPatient,
                                appointment.getStaff(), appointment.getStartHour(), clock,
                                isTwistActive(TwistMode.TWIST_3_DISCOUNTS));
                    }
                }
                int now = currentHour();
                if (waitingPatient != null) {
                    log.logSuccess("Appointment #" + appointmentId + " cancelled at hour " + now
                            + " (Reason: " + reason + "); waiting list offered the slot to patient #"
                            + waitingPatient.getId() + " as PENDING appointment #" + newAppointmentId + ".");
                } else {
                    log.logSuccess("Appointment #" + appointmentId + " cancelled at hour " + now
                            + " (Reason: " + reason + ")");
                }
                appointment.cancel(reason);
                if (waitingPatient != null) {
                    staffWaitingLists.get(appointment.getStaff().getId()).remove(waitingPatient);
                    appointmentsById.put(newAppointmentId, waitingAppointment);
                    appointmentList.add(waitingAppointment);
                }
            } catch (RuntimeException ex) {
                recordFailure(log, ex);
                throw ex;
            }
        }
    }

    private Patient findWaitingPatientForSlot(Appointment cancelledAppt) {
        int staffId = cancelledAppt.getStaff().getId();
        int slotHour = cancelledAppt.getStartHour();
        List<Patient> waitList = staffWaitingLists.get(staffId);
        if (waitList == null || waitList.isEmpty()) {
            return null;
        }

        for (Patient candidate : waitList) {
            Appointment patientConflict = findNonCancelledAppointmentForPatientAt(candidate.getId(), slotHour);
            if (patientConflict != null && patientConflict != cancelledAppt) {
                continue;
            }
            try {
                if (isTwistActive(TwistMode.TWIST_2_BUFFER)) {
                    checkStaffBufferConstraint(cancelledAppt.getStaff(), slotHour, cancelledAppt);
                }
                return candidate;
            } catch (ConflictException ignored) {
                // Try the next waiting patient if this slot violates the staff buffer.
            }
        }
        return null;
    }

    // =========================================================================
    // TWIST 0 WAITING LIST MANAGEMENT
    // =========================================================================

    /**
     * Adds a patient to a staff member's waiting list (Twist 0).
     *
     * @param staffId ID of the staff member
     * @param patientId ID of the patient
     */
    public synchronized void joinWaitingList(int staffId, int patientId) {
        String opName = "waiting list: patient #" + patientId + " -> staff #" + staffId;
        try (AppointmentLog log = new AppointmentLog(clock, opName)) {
            try {
                Patient patient = getExistingPatient(patientId);
                Staff staff = getExistingStaff(staffId);
                List<Patient> queue = staffWaitingLists.get(staffId);

                if (queue.contains(patient)) {
                    throw new ConflictException("Patient " + patient.getName() + " is already on the waiting list for " + staff.getName());
                }

                log.logSuccess("Patient " + patient.getName() + " (#" + patientId + ") added to waiting list for " + staff.role() + " " + staff.getName());
                queue.add(patient);
            } catch (RuntimeException ex) {
                recordFailure(log, ex);
                throw ex;
            }
        }
    }

    /**
     * Returns an unmodifiable list of patients waiting for a specific staff member.
     *
     * @param staffId staff ID
     * @return unmodifiable list of waiting patients
     */
    public List<Patient> getWaitingListForStaff(int staffId) {
        List<Patient> list = staffWaitingLists.get(staffId);
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(list));
    }

    // =========================================================================
    // POLYMORPHIC STAFF FEE PRINTING (Assignment Question 3)
    // =========================================================================

    /**
     * Prints each staff member's role and fee in a single loop.
     *
     * Polymorphic Guarantee:
     * Resolves each role and fee through ordinary dynamic dispatch.
     * New Staff subclasses can be added without modifying this loop or Clinic.
     *
     * @param out print stream to output to
     */
    public void printStaffRolesAndFees(PrintStream out) {
        out.println("-------------------------------------------------------------------");
        out.printf("%-10s %-20s %-20s %-12s%n", "Staff ID", "Name", "Role", "Fee");
        out.println("-------------------------------------------------------------------");
        List<Staff> staffMembers = getStaffMembers();
        for (Staff staff : staffMembers) {
            out.printf("#%-9d %-20s %-20s $%-11.2f%n",
                    staff.getId(),
                    staff.getName(),
                    staff.role(),
                    staff.fee());
        }
        out.println("-------------------------------------------------------------------");
    }

    // =========================================================================
    // LOOKUPS, VALIDATIONS, AND CONFLICT DETECTORS
    // =========================================================================

    public Patient getExistingPatient(int patientId) {
        Patient patient = patientsById.get(patientId);
        if (patient == null) {
            throw new ValidationException("Patient with ID #" + patientId + " not found.");
        }
        return patient;
    }

    public Staff getExistingStaff(int staffId) {
        Staff staff = staffById.get(staffId);
        if (staff == null) {
            throw new ValidationException("Staff with ID #" + staffId + " not found.");
        }
        return staff;
    }

    public Appointment getExistingAppointment(int appointmentId) {
        Appointment appointment = appointmentsById.get(appointmentId);
        if (appointment == null) {
            throw new ValidationException("Appointment with ID #" + appointmentId + " not found.");
        }
        return appointment;
    }

    private void validateAppointmentHour(int hour) {
        if (hour < 0 || hour >= 168) {
            throw new ValidationException("Appointment hour must be within weekly schedule [0, 167]. Provided: " + hour);
        }
        int now = currentHour();
        if (hour < now) {
            throw new ValidationException("Cannot book an appointment in the past (Clinic time: " + now + ", booking hour: " + hour + ").");
        }
    }

    public Appointment findNonCancelledAppointmentForStaffAt(int staffId, int hour) {
        for (Appointment appt : appointmentList) {
            if (appt.getStaff().getId() == staffId
                    && appt.getStartHour() == hour
                    && appt.getStatus() != AppointmentStatus.CANCELLED) {
                return appt;
            }
        }
        return null;
    }

    public Appointment findNonCancelledAppointmentForPatientAt(int patientId, int hour) {
        for (Appointment appt : appointmentList) {
            if (appt.getPatient().getId() == patientId
                    && appt.getStartHour() == hour
                    && appt.getStatus() != AppointmentStatus.CANCELLED) {
                return appt;
            }
        }
        return null;
    }

    // =========================================================================
    // ENCAPSULATION & DEFENSIVE ACCESSORS
    // =========================================================================

    public List<Patient> getPatients() {
        return Collections.unmodifiableList(new ArrayList<>(patientsById.values()));
    }

    public List<Staff> getStaffMembers() {
        return Collections.unmodifiableList(new ArrayList<>(staffById.values()));
    }

    public List<Appointment> getAppointments() {
        return Collections.unmodifiableList(new ArrayList<>(appointmentList));
    }

    public List<LogEntry> getAuditLogs() {
        return AppointmentLog.getHistory();
    }

    public Clock getClock() {
        return clock;
    }

    public TwistMode getTwistMode() {
        return twistMode;
    }

    public synchronized void setTwistMode(TwistMode twistMode) {
        try (AppointmentLog log = new AppointmentLog(clock, "change twist mode")) {
            try {
                TwistMode requestedMode = Objects.requireNonNull(twistMode, "TwistMode cannot be null.");
                log.logSuccess("Twist mode changed to " + requestedMode.name());
                this.twistMode = requestedMode;
            } catch (RuntimeException ex) {
                recordFailure(log, ex);
                throw ex;
            }
        }
    }

    /**
     * Checks if a given twist is currently enabled under the current mode.
     */
    public boolean isTwistActive(TwistMode mode) {
        if (this.twistMode == TwistMode.ALL_ENABLED) {
            return true;
        }
        return this.twistMode == mode;
    }
}
