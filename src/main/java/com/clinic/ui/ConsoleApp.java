package com.clinic.ui;

import com.clinic.clock.SimulatedClock;
import com.clinic.config.ClinicConfig;
import com.clinic.config.TwistMode;
import com.clinic.exception.CancellationException;
import com.clinic.exception.ClinicException;
import com.clinic.exception.ConflictException;
import com.clinic.log.AppointmentLog;
import com.clinic.log.LogEntry;
import com.clinic.model.*;
import com.clinic.service.Clinic;

import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/**
 * Robust, user-friendly, menu-driven console interface for the Clinic Appointment System.
 * Designed for beginners: simple English, clear prompts, robust error handling,
 * and zero crashes on invalid user input.
 */
public class ConsoleApp {

    private final Clinic clinic;
    private final SimulatedClock clock;
    private final Scanner scanner;
    private final PrintStream out;

    public ConsoleApp() {
        this(new SimulatedClock(10), System.out, new Scanner(System.in));
    }

    public ConsoleApp(SimulatedClock clock, PrintStream out, Scanner scanner) {
        this.clock = clock;
        this.out = out;
        this.scanner = scanner;
        this.clinic = new Clinic(this.clock, ClinicConfig.getActiveTwist());
        seedInitialSampleData();
    }

    /**
     * Seeds friendly starting data so the user can immediately test operations
     * without having to manually type multiple records first.
     */
    private void seedInitialSampleData() {
        // Register sample staff members
        clinic.registerStaff(new Doctor(101, "Dr. Alice Smith"));
        clinic.registerStaff(new Nurse(102, "Nurse Bob Jones"));
        clinic.registerStaff(new Pharmacist(103, "Pharm. Clara Oswald"));
        clinic.registerStaff(new MedicalLecturer(104, "Prof. Gregory House", "Clinical Diagnostics"));

        // Register sample patients
        clinic.registerPatient(new VisitorPatient(201, "David Evans"));
        clinic.registerPatient(new StudentPatient(202, "Emma Watson"));
        clinic.registerPatient(new StaffPatient(203, "Frank Castle"));
    }

    /**
     * Main interactive event loop.
     */
    public void start() {
        out.println("===============================================================================");
        out.println("            WELCOME TO THE CLINIC APPOINTMENT SYSTEM (JAVA 21)                 ");
        out.println("              Computer Science Mini Project - OOP Demonstration               ");
        out.println("===============================================================================");

        boolean running = true;
        while (running) {
            printCurrentStatusHeader();
            printMenu();
            int choice = readInt("Select an option (1-12): ", 1, 12);

            switch (choice) {
                case 1 -> handleRegisterPatient();
                case 2 -> handleRegisterStaff();
                case 3 -> handleViewPatientsAndStaff();
                case 4 -> handleBookAppointment();
                case 5 -> handleConfirmAppointment();
                case 6 -> handleCancelAppointment();
                case 7 -> handleViewAppointments();
                case 8 -> handleViewLogs();
                case 9 -> handleDemonstrateTwists();
                case 10 -> handleClockControls();
                case 11 -> runFiveMinuteDemo();
                case 12 -> {
                    out.println("\nThank you for using the Clinic Appointment System. Goodbye!");
                    running = false;
                }
            }
        }
    }

    private void printCurrentStatusHeader() {
        out.println("\n-------------------------------------------------------------------------------");
        out.printf("  CLINIC CLOCK: Hour %03d / 168  |  ACTIVE TWIST: %s%n",
                clock.now(), clinic.getTwistMode().name());
        out.println("-------------------------------------------------------------------------------");
    }

    private void printMenu() {
        out.println("1.  Register a Patient (Visitor, Student, Staff-Member, Emergency)");
        out.println("2.  Register Staff (Doctor, Nurse, Pharmacist, Medical Lecturer)");
        out.println("3.  View Patients & Staff (Polymorphic Roles & Fees Loop)");
        out.println("4.  Book an Appointment");
        out.println("5.  Confirm an Appointment");
        out.println("6.  Cancel an Appointment (Enforces 2-hour rule & Waiting List)");
        out.println("7.  View Appointments (Status, Duration, and Polymorphic Price)");
        out.println("8.  View Appointment Logs (Audit Trail)");
        out.println("9.  Demonstrate & Configure Twists (Twist 0, 1, 2, 3)");
        out.println("10. Advance / Set Clinic Clock (Test Timing Rules)");
        out.println("11. Run Automated 5-Minute Demonstration");
        out.println("12. Exit");
        out.println("-------------------------------------------------------------------------------");
    }

    // =========================================================================
    // MENU HANDLERS
    // =========================================================================

    private void handleRegisterPatient() {
        out.println("\n--- [1] Register New Patient ---");
        int id = readPositiveInt("Enter new Patient ID (> 0): ");
        String name = readNonEmptyString("Enter Patient Full Name: ");

        out.println("Select Patient Category (Twist 3 Discounts):");
        out.println("1. Visitor Patient (Standard 100% fee)");
        out.println("2. Student Patient (50% discount)");
        out.println("3. University Staff Patient (Free / 0% fee)");
        int type = readInt("Choice (1-3): ", 1, 3);

        boolean emergency = readBoolean("Is this an emergency triage patient? (y/n): ");

        Patient patient;
        switch (type) {
            case 2 -> patient = new StudentPatient(id, name, emergency);
            case 3 -> patient = new StaffPatient(id, name, emergency);
            default -> patient = new VisitorPatient(id, name, emergency);
        }

        try {
            clinic.registerPatient(patient);
            out.println("[SUCCESS] " + patient.role() + " successfully registered!");
        } catch (ClinicException ex) {
            out.println("[ERROR] Failed to register patient: " + ex.getMessage());
        }
    }

    private void handleRegisterStaff() {
        out.println("\n--- [2] Register New Staff Member ---");
        int id = readPositiveInt("Enter new Staff ID (> 0): ");
        String name = readNonEmptyString("Enter Staff Full Name: ");

        out.println("Select Staff Role:");
        out.println("1. Doctor (Fee: 40 + 5*(P%5), Buffer: 2h)");
        out.println("2. Nurse (Fee: 10 + (P%3), Buffer: 1h)");
        out.println("3. Pharmacist (Fee: 5, Buffer: 0h)");
        out.println("4. Medical Lecturer (Doctor + Academic Lecturer)");
        int role = readInt("Choice (1-4): ", 1, 4);

        Staff staff = null;
        switch (role) {
            case 1 -> staff = new Doctor(id, name);
            case 2 -> staff = new Nurse(id, name);
            case 3 -> staff = new Pharmacist(id, name);
            case 4 -> {
                String module = readNonEmptyString("Enter Academic Module Name (e.g. Pathology 101): ");
                staff = new MedicalLecturer(id, name, module);
            }
        }

        try {
            clinic.registerStaff(staff);
            out.println("[SUCCESS] " + staff.role() + " successfully registered!");
        } catch (ClinicException ex) {
            out.println("[ERROR] Failed to register staff: " + ex.getMessage());
        }
    }

    private void handleViewPatientsAndStaff() {
        out.println("\n--- [3] Clinic Directory ---");

        out.println("\n[Registered Patients]");
        List<Patient> patients = clinic.getPatients();
        if (patients.isEmpty()) {
            out.println("No patients registered yet.");
        } else {
            out.printf("%-10s %-25s %-25s %-12s%n", "Patient ID", "Name", "Role", "Discount Tier");
            out.println("-------------------------------------------------------------------------------");
            for (Patient p : patients) {
                out.printf("#%-9d %-25s %-25s %-12s%n",
                        p.getId(), p.getName(), p.role(),
                        String.format("%.0f%%", (1.0 - p.discountMultiplier()) * 100));
            }
        }

        out.println("\n[Registered Staff Members & Polymorphic Consultation Fees]");
        // Demonstrates the polymorphic staff fee loop.
        clinic.printStaffRolesAndFees(out);
    }

    private void handleBookAppointment() {
        out.println("\n--- [4] Book an Appointment ---");
        int patientId = readPositiveInt("Enter Patient ID: ");
        int staffId = readPositiveInt("Enter Staff ID: ");
        int startHour = readInt("Enter Start Hour (0 to 167, Current clock is " + clock.now() + "): ", 0, 167);

        try {
            Appointment appt = clinic.bookAppointment(patientId, staffId, startHour);
            out.println("[SUCCESS] " + appt);
        } catch (ClinicException ex) {
            out.println("[BOOKING REJECTED] " + ex.getMessage());
        }
    }

    private void handleConfirmAppointment() {
        out.println("\n--- [5] Confirm an Appointment ---");
        int apptId = readPositiveInt("Enter Appointment ID to confirm: ");
        try {
            clinic.confirmAppointment(apptId);
            out.println("[SUCCESS] Appointment #" + apptId + " is now CONFIRMED.");
        } catch (Exception ex) {
            out.println("[CONFIRMATION FAILED] " + ex.getMessage());
        }
    }

    private void handleCancelAppointment() {
        out.println("\n--- [6] Cancel an Appointment ---");
        int apptId = readPositiveInt("Enter Appointment ID to cancel: ");
        String reason = readNonEmptyString("Enter reason for cancellation: ");
        try {
            clinic.cancelAppointment(apptId, reason);
            out.println("[SUCCESS] Appointment #" + apptId + " was cancelled.");
        } catch (Exception ex) {
            out.println("[CANCELLATION REJECTED] " + ex.getMessage());
        }
    }

    private void handleViewAppointments() {
        out.println("\n--- [7] Clinic Appointments ---");
        List<Appointment> list = clinic.getAppointments();
        if (list.isEmpty()) {
            out.println("No appointments booked yet.");
            return;
        }

        for (Appointment a : list) {
            out.println(a);
        }
    }

    private void handleViewLogs() {
        out.println("\n--- [8] Appointment Audit Trail (Log History) ---");
        List<LogEntry> logs = clinic.getAuditLogs();
        if (logs.isEmpty()) {
            out.println("Log is empty.");
            return;
        }
        for (LogEntry entry : logs) {
            out.println(entry);
        }
    }

    private void handleDemonstrateTwists() {
        out.println("\n--- [9] Demonstrate & Configure Twists ---");
        out.println("Select Twist Configuration Mode:");
        out.println("0. Twist 0: Waiting List (auto-offer cancelled slots)");
        out.println("1. Twist 1: Emergency (bump PENDING appointments)");
        out.println("2. Twist 2: Buffer (Doctors 2h, Nurses 1h, Pharmacists 0h)");
        out.println("3. Twist 3: Discounts (Students 50%, Staff 0%, Visitors 100%)");
        out.println("4. Standard Mode (Twists disabled)");
        out.println("5. ALL Twists Enabled (Combined mode)");
        out.println("6. Join Staff Waiting List (Twist 0 action)");
        out.println("7. View Waiting Lists (Twist 0 action)");

        int choice = readInt("Select option (0-7): ", 0, 7);
        switch (choice) {
            case 0 -> {
                clinic.setTwistMode(TwistMode.TWIST_0_WAITING_LIST);
                out.println("[CONFIG] Active twist set to: " + TwistMode.TWIST_0_WAITING_LIST.getDescription());
            }
            case 1 -> {
                clinic.setTwistMode(TwistMode.TWIST_1_EMERGENCY);
                out.println("[CONFIG] Active twist set to: " + TwistMode.TWIST_1_EMERGENCY.getDescription());
            }
            case 2 -> {
                clinic.setTwistMode(TwistMode.TWIST_2_BUFFER);
                out.println("[CONFIG] Active twist set to: " + TwistMode.TWIST_2_BUFFER.getDescription());
            }
            case 3 -> {
                clinic.setTwistMode(TwistMode.TWIST_3_DISCOUNTS);
                out.println("[CONFIG] Active twist set to: " + TwistMode.TWIST_3_DISCOUNTS.getDescription());
            }
            case 4 -> {
                clinic.setTwistMode(TwistMode.STANDARD);
                out.println("[CONFIG] Active twist set to: " + TwistMode.STANDARD.getDescription());
            }
            case 5 -> {
                clinic.setTwistMode(TwistMode.ALL_ENABLED);
                out.println("[CONFIG] Active twist set to: " + TwistMode.ALL_ENABLED.getDescription());
            }
            case 6 -> {
                int staffId = readPositiveInt("Enter Staff ID: ");
                int patientId = readPositiveInt("Enter Patient ID to join waiting list: ");
                try {
                    clinic.joinWaitingList(staffId, patientId);
                    out.println("[SUCCESS] Patient joined waiting list.");
                } catch (Exception ex) {
                    out.println("[FAILED] " + ex.getMessage());
                }
            }
            case 7 -> {
                int staffId = readPositiveInt("Enter Staff ID to inspect waiting list: ");
                List<Patient> waitList = clinic.getWaitingListForStaff(staffId);
                out.println("Waiting List for Staff #" + staffId + ":");
                if (waitList.isEmpty()) {
                    out.println("  (List is empty)");
                } else {
                    for (int i = 0; i < waitList.size(); i++) {
                        Patient p = waitList.get(i);
                        out.printf("  %d. [%s #%d] %s%n", (i + 1), p.role(), p.getId(), p.getName());
                    }
                }
            }
        }
    }

    private void handleClockControls() {
        out.println("\n--- [10] Clinic Clock Simulation ---");
        out.println("Current Clock Hour: " + clock.now() + " / 168");
        out.println("1. Advance Clock by X Hours");
        out.println("2. Set Clock to Specific Hour (0 to 168)");
        int choice = readInt("Choice (1-2): ", 1, 2);

        if (choice == 1) {
            int advanceHours = readPositiveInt("Enter hours to advance: ");
            try {
                clock.advance(advanceHours);
                out.println("[CLOCK UPDATED] New clinic time: Hour " + clock.now() + " / 168");
            } catch (Exception ex) {
                out.println("[ERROR] " + ex.getMessage());
            }
        } else {
            int newHour = readInt("Enter target hour (0-168): ", 0, 168);
            try {
                clock.setTime(newHour);
                out.println("[CLOCK UPDATED] New clinic time: Hour " + clock.now() + " / 168");
            } catch (Exception ex) {
                out.println("[ERROR] " + ex.getMessage());
            }
        }
    }

    // =========================================================================
    // AUTOMATED 5-MINUTE COMPREHENSIVE DEMONSTRATION
    // =========================================================================

    /**
     * Executes the exact demonstration required by Section 12:
     * 1. Creating a patient and staff member.
     * 2. Booking an appointment.
     * 3. Confirming an appointment.
     * 4. Attempting an invalid booking (deliberate failure: double booking / buffer).
     * 5. Attempting an invalid cancellation (deliberate failure: 2-hour rule violation).
     * 6. Showing the appointment log with [log] opened, [log] closed, and FAILED entries.
     * 7. Demonstrating the assigned twists.
     */
    public void runFiveMinuteDemo() {
        out.println("\n===============================================================================");
        out.println("          STARTING AUTOMATED 5-MINUTE LIVE SYSTEM DEMONSTRATION                ");
        out.println("===============================================================================");

        SimulatedClock demoClock = new SimulatedClock(10); // Start at Hour 10
        Clinic demoClinic = new Clinic(demoClock, TwistMode.ALL_ENABLED);

        out.println("\n>>> STEP 1: Creating Patients and Staff Members (Encapsulation & Validation)");
        Patient patientSarah = new VisitorPatient(1, "Sarah Connor");
        Patient patientJohn = new StudentPatient(2, "John Connor");
        Staff doctorHouse = new Doctor(10, "Dr. Gregory House");
        Staff nurseJoy = new Nurse(20, "Nurse Joy");
        Staff pharmBob = new Pharmacist(30, "Pharmacist Bob");
        Person.resetConstructorCallCount();
        MedicalLecturer profWatson = new MedicalLecturer(40, "Prof. John Watson", "Forensic Pathology");

        demoClinic.registerPatient(patientSarah);
        demoClinic.registerPatient(patientJohn);
        demoClinic.registerStaff(doctorHouse);
        demoClinic.registerStaff(nurseJoy);
        demoClinic.registerStaff(pharmBob);
        demoClinic.registerStaff(profWatson);

        out.println("\nDemonstrating Multiple Inheritance for MedicalLecturer:");
        out.println("  Role: " + profWatson.role());
        out.println("  Module: " + profWatson.getModuleName());
        out.println("  Consultation Fee: $" + profWatson.fee());
        out.println("  Person constructor invocation count: " + Person.getConstructorCallCount());

        out.println("\n>>> STEP 2: Booking an Appointment");
        out.println("Booking Sarah with Dr. House at Hour 15 (Current clock: " + demoClock.now() + ")...");
        Appointment appt1 = demoClinic.bookAppointment(patientSarah.getId(), doctorHouse.getId(), 15);
        out.println("  Result: " + appt1);

        out.println("\n>>> STEP 3: Confirming an Appointment");
        out.println("Confirming Appointment #" + appt1.getId() + " at Hour 10...");
        demoClinic.confirmAppointment(appt1.getId());
        out.println("  Status after confirmation: " + appt1.getStatus());

        out.println("\n>>> STEP 4: Attempting an Invalid Booking (DELIBERATE FAILURE #1: Conflict / Double Booking)");
        out.println("Attempting to book John with Dr. House at the same Hour 15...");
        try {
            demoClinic.bookAppointment(patientJohn.getId(), doctorHouse.getId(), 15);
            out.println("  [ERROR] Conflict check failed to reject booking!");
        } catch (ConflictException ex) {
            out.println("  [EXPECTED CATCH] Cleanly rejected double booking: " + ex.getMessage());
        }

        out.println("\n>>> STEP 5: Attempting an Invalid Cancellation (DELIBERATE FAILURE #2: 2-Hour Rule Violation)");
        out.println("Advancing clock to Hour 14 (1 hour before start hour 15)...");
        demoClock.setTime(14);
        out.println("Current clock: " + demoClock.now() + " | Appointment #" + appt1.getId() + " starts at: " + appt1.getStartHour());
        out.println("Attempting to cancel confirmed appointment #" + appt1.getId() + " less than 2 hours before start...");
        try {
            demoClinic.cancelAppointment(appt1.getId(), "Patient had lunch");
            out.println("  [ERROR] Timing check failed to reject late cancellation!");
        } catch (CancellationException ex) {
            out.println("  [EXPECTED CATCH] Cleanly rejected cancellation: " + ex.getMessage());
        }

        out.println("\n>>> STEP 6: Valid Cancellation & Twist 0 (Waiting List Auto-Allocation)");
        out.println("Resetting clock to Hour 12 (3 hours before start hour 15 -> valid cancellation window)");
        demoClock.setTime(12);
        out.println("John Connor joins Dr. House's waiting list...");
        demoClinic.joinWaitingList(doctorHouse.getId(), patientJohn.getId());

        out.println("Sarah cancels Appointment #" + appt1.getId() + " (gap is 3 hours >= 2 hours)...");
        demoClinic.cancelAppointment(appt1.getId(), "Family emergency");
        out.println("Appointment #" + appt1.getId() + " status: " + appt1.getStatus() + " (" + appt1.getCancellationReason() + ")");

        out.println("\nInspecting all appointments to verify Twist 0 auto-offered the slot:");
        for (Appointment a : demoClinic.getAppointments()) {
            out.println("  " + a);
        }

        out.println("\n>>> STEP 7: Demonstrating Twist 1 (Emergency Bumping)");
        out.println("Nurse Joy has a PENDING appointment with Sarah at Hour 20...");
        Appointment nurseAppt = demoClinic.bookAppointment(patientSarah.getId(), nurseJoy.getId(), 20);
        out.println("  Created: " + nurseAppt);

        out.println("Registering Emergency Patient Bruce Wayne...");
        Patient bruce = new VisitorPatient(99, "Bruce Wayne", true);
        demoClinic.registerPatient(bruce);

        out.println("Emergency patient Bruce Wayne books Nurse Joy at the exact same Hour 20...");
        Appointment emergAppt = demoClinic.bookAppointment(bruce.getId(), nurseJoy.getId(), 20);
        out.println("  Emergency booking succeeded: " + emergAppt);
        out.println("  Previous appointment status: " + nurseAppt.getStatus() + " (Reason: " + nurseAppt.getCancellationReason() + ")");

        out.println("\n>>> STEP 8: Demonstrating Twist 2 (Staff Buffer Hours Polymorphism)");
        out.println("Doctors require 2h buffer, Nurses 1h, Pharmacists 0h.");
        out.println("Dr. House is booked at Hour 15. Attempting to book Dr. House at Hour 16 (gap = 0h < 2h)...");
        try {
            demoClinic.bookAppointment(patientSarah.getId(), doctorHouse.getId(), 16);
            out.println("  [ERROR] Buffer check failed!");
        } catch (ConflictException ex) {
            out.println("  [EXPECTED CATCH] Buffer rule enforced: " + ex.getMessage());
        }

        out.println("\n>>> STEP 9: Demonstrating Twist 3 (Patient Discounts Polymorphism)");
        out.println("Comparing appointment charges for Dr. House ($" + doctorHouse.fee() + " base fee):");
        Patient visitor = new VisitorPatient(301, "Visitor Vic");
        Patient student = new StudentPatient(302, "Student Sam");
        Patient staffPat = new StaffPatient(303, "Staff Sally");

        out.printf("  Visitor (100%%): $%.2f%n", visitor.calculateCharge(doctorHouse.fee()));
        out.printf("  Student (50%%):  $%.2f%n", student.calculateCharge(doctorHouse.fee()));
        out.printf("  Staff   (0%%):   $%.2f%n", staffPat.calculateCharge(doctorHouse.fee()));

        out.println("\n>>> STEP 10: Appointment Audit Log (Showing [log] opened, closed, and FAILED entries)");
        out.println("Total log entries captured: " + demoClinic.getAuditLogs().size());
        out.println("Sample logged actions:");
        List<LogEntry> logs = demoClinic.getAuditLogs();
        int showCount = Math.min(logs.size(), 8);
        for (int i = 0; i < showCount; i++) {
            out.println("  " + logs.get(i));
        }
        out.println("  ... (and " + (logs.size() - showCount) + " more log records).");

        out.println("\n===============================================================================");
        out.println("                  DEMONSTRATION COMPLETED SUCCESSFULLY!                        ");
        out.println("===============================================================================\n");
    }

    // =========================================================================
    // ROBUST USER INPUT HELPERS (Crash-Proof)
    // =========================================================================

    private int readInt(String prompt, int min, int max) {
        while (true) {
            out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
                out.printf("[INVALID] Please enter an integer between %d and %d.%n", min, max);
            } catch (NumberFormatException e) {
                out.println("[INVALID] Input must be a valid whole number. Try again.");
            }
        }
    }

    private int readPositiveInt(String prompt) {
        while (true) {
            out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value > 0) {
                    return value;
                }
                out.println("[INVALID] Value must be strictly positive (> 0). Try again.");
            } catch (NumberFormatException e) {
                out.println("[INVALID] Input must be a positive integer. Try again.");
            }
        }
    }

    private String readNonEmptyString(String prompt) {
        while (true) {
            out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            out.println("[INVALID] Text cannot be empty or blank. Try again.");
        }
    }

    private boolean readBoolean(String prompt) {
        while (true) {
            out.print(prompt);
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("y") || input.equals("yes") || input.equals("true") || input.equals("1")) {
                return true;
            }
            if (input.equals("n") || input.equals("no") || input.equals("false") || input.equals("0")) {
                return false;
            }
            out.println("[INVALID] Please type 'y' (yes) or 'n' (no).");
        }
    }
}
