package com.clinic.model;

import com.clinic.clock.Clock;
import com.clinic.exception.CancellationException;
import com.clinic.exception.ValidationException;

/**
 * =========================================================================
 * CLASS INVARIANTS FOR Appointment:
 * 1. Identifier Integrity: id > 0 and immutable.
 * 2. Entity Associations: patient and staff are non-null and immutable.
 * 3. Temporal Bounds: 0 <= startHour < 168, and duration is exactly 1 hour;
 *    hence endHour == startHour + 1 <= 168.
 * 4. Status Encapsulation: status is private and cannot be set directly.
 *    Transitions occur strictly via confirm() and cancel() business methods.
 * 5. State Machine Constraints:
 *    - Initial state is always PENDING.
 *    - CANCELLED is a terminal sink state; once CANCELLED, an appointment
 *      can never transition to any other status.
 *    - No appointment can be cancelled once clock.now() >= startHour.
 *    - A confirmed appointment can be cancelled only when startHour - clock.now() >= 2.
 *    - A pending appointment may be cancelled any time before it starts.
 * 6. Cancellation Reason: recorded only upon reaching CANCELLED status.
 * =========================================================================
 *
 * Represents an individual 1-hour appointment booking within the clinic.
 */
public class Appointment {

    private final int id;
    private final Patient patient;
    private final Staff staff;
    private final int startHour;
    private final Clock clock;
    private final boolean discountsEnabled;
    private AppointmentStatus status;
    private String cancellationReason;

    /**
     * Constructs a new appointment in PENDING status.
     *
     * @param id unique positive appointment ID
     * @param patient non-null patient
     * @param staff non-null attending staff member
     * @param startHour whole hour between 0 and 167
     * @param clock injected clinic clock
     * @throws ValidationException if arguments violate domain invariants
     */
    public Appointment(int id, Patient patient, Staff staff, int startHour, Clock clock) {
        this(id, patient, staff, startHour, clock, true);
    }

    /**
     * Constructs an appointment with an explicit clinic pricing mode.
     *
     * @param id unique positive appointment ID
     * @param patient non-null patient
     * @param staff non-null attending staff member
     * @param startHour whole hour between 0 and 167
     * @param clock injected clinic clock
     * @param discountsEnabled whether Twist 3 pricing is active for this booking
     * @throws ValidationException if arguments violate domain invariants
     */
    public Appointment(int id, Patient patient, Staff staff, int startHour, Clock clock,
                       boolean discountsEnabled) {
        if (id <= 0) {
            throw new ValidationException("Appointment ID must be positive (> 0). Provided: " + id);
        }
        if (patient == null) {
            throw new ValidationException("Appointment patient must not be null.");
        }
        if (staff == null) {
            throw new ValidationException("Appointment staff must not be null.");
        }
        if (clock == null) {
            throw new ValidationException("Appointment clock must not be null.");
        }
        if (startHour < 0 || startHour >= 168) {
            throw new ValidationException("Start hour must be between 0 and 167 (1-hour duration within 168-hour week). Provided: " + startHour);
        }

        this.id = id;
        this.patient = patient;
        this.staff = staff;
        this.startHour = startHour;
        this.clock = clock;
        this.discountsEnabled = discountsEnabled;
        this.status = AppointmentStatus.PENDING;
        this.cancellationReason = null;
    }

    /**
     * Confirms a PENDING appointment.
     *
     * @throws CancellationException or IllegalStateException if cannot be confirmed
     */
    public void confirm() {
        validateConfirmation();
        this.status = AppointmentStatus.CONFIRMED;
    }

    /** Checks whether this appointment may be confirmed without changing it. */
    public void validateConfirmation() {
        int currentHour = currentHour();
        if (status == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("A cancelled appointment can never return to another status.");
        }
        if (status == AppointmentStatus.CONFIRMED) {
            return; // Idempotent confirm
        }
        if (currentHour >= startHour) {
            throw new IllegalStateException("Cannot confirm an appointment that has already started or elapsed (Current hour: "
                    + currentHour + ", start hour: " + startHour + ").");
        }
    }

    /**
     * Cancels this appointment in accordance with clinic timing rules.
     *
     * Rules enforced:
     * 1. A cancelled appointment can never return to another status.
     * 2. No appointment can be cancelled once it has started (currentHour >= startHour).
     * 3. A confirmed appointment cannot be cancelled less than 2 hours before its start.
     * 4. A pending appointment may be cancelled before it starts.
     *
     * @param reason non-empty reason for cancellation
     * @throws CancellationException if timing rules are violated
     */
    public void cancel(String reason) {
        validateCancellation();
        this.status = AppointmentStatus.CANCELLED;
        this.cancellationReason = (reason == null || reason.trim().isEmpty()) ? "Unspecified reason" : reason.trim();
    }

    /**
     * Checks cancellation timing without changing appointment state.
     * Clinic uses this before recording an operation's success.
     */
    public void validateCancellation() {
        int currentHour = currentHour();
        if (status == AppointmentStatus.CANCELLED) {
            throw new CancellationException("A cancelled appointment can never return to another status.");
        }
        if (currentHour >= startHour) {
            throw new CancellationException("No appointment can be cancelled once it has started (Current: "
                    + currentHour + ", Start: " + startHour + ").");
        }
        if (status == AppointmentStatus.CONFIRMED && (startHour - currentHour) < 2) {
            throw new CancellationException("A confirmed appointment cannot be cancelled less than two hours before start (Current: "
                    + currentHour + ", Start: " + startHour + ", Gap: " + (startHour - currentHour) + "h).");
        }
    }

    private int currentHour() {
        int now = clock.now();
        if (now < 0 || now > 168) {
            throw new IllegalStateException("Clock.now() must return an hour between 0 and 168; got " + now + ".");
        }
        return now;
    }

    public int getId() {
        return id;
    }

    public Patient getPatient() {
        return patient;
    }

    public Staff getStaff() {
        return staff;
    }

    public int getStartHour() {
        return startHour;
    }

    public int getEndHour() {
        return startHour + 1;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    /**
     * Calculates the fee charged for this appointment polymorphically.
     * Combines the staff consultation fee with the patient discount tier.
     * The patient and staff policies resolve the price polymorphically.
     *
     * @return final calculated fee
     */
    public double calculatePrice() {
        double baseFee = staff.fee();
        return discountsEnabled ? patient.calculateCharge(baseFee) : baseFee;
    }

    @Override
    public String toString() {
        String info = String.format("Appointment #%d [%s] Hour %d-%d | Patient: %s (#%d) | Staff: %s (#%d, %s) | Fee: $%.2f",
                id, status, startHour, getEndHour(),
                patient.getName(), patient.getId(),
                staff.getName(), staff.getId(), staff.role(),
                calculatePrice());
        if (status == AppointmentStatus.CANCELLED && cancellationReason != null) {
            info += " (Reason: " + cancellationReason + ")";
        }
        return info;
    }
}
