package com.clinic;

import com.clinic.clock.FixedClock;
import com.clinic.config.TwistMode;
import com.clinic.model.*;
import com.clinic.service.Clinic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Twist 0: Waiting List Tests")
class TwistWaitingListTest {

    private Clinic clinic;
    private Patient patientSarah;
    private Patient patientJohn;
    private Patient patientDavid;
    private Staff doctor;

    @BeforeEach
    void setUp() {
        clinic = new Clinic(new FixedClock(10), TwistMode.TWIST_0_WAITING_LIST);
        patientSarah = new VisitorPatient(1, "Sarah Connor");
        patientJohn = new VisitorPatient(2, "John Connor");
        patientDavid = new VisitorPatient(3, "David Evans");
        doctor = new Doctor(10, "Dr. House");

        clinic.registerPatient(patientSarah);
        clinic.registerPatient(patientJohn);
        clinic.registerPatient(patientDavid);
        clinic.registerStaff(doctor);
    }

    @Test
    @DisplayName("Cancelled slot is automatically offered to first waiting patient with no conflict")
    void cancelledSlotAutoOfferedToWaitingPatient() {
        // Sarah books doctor at hour 15
        Appointment appt1 = clinic.bookAppointment(patientSarah.getId(), doctor.getId(), 15);

        // John Connor joins Dr. House's waiting list
        clinic.joinWaitingList(doctor.getId(), patientJohn.getId());
        assertEquals(1, clinic.getWaitingListForStaff(doctor.getId()).size());

        // Sarah cancels Appointment #1 (clock is 10, hour is 15 -> valid)
        clinic.cancelAppointment(appt1.getId(), "Patient travel");
        assertEquals(AppointmentStatus.CANCELLED, appt1.getStatus());

        // Verify John was removed from the waiting list
        assertEquals(0, clinic.getWaitingListForStaff(doctor.getId()).size());

        // Verify a new PENDING appointment was automatically created for John at hour 15
        List<Appointment> allAppts = clinic.getAppointments();
        assertEquals(2, allAppts.size());

        Appointment newAppt = allAppts.get(1);
        assertEquals(patientJohn.getId(), newAppt.getPatient().getId());
        assertEquals(doctor.getId(), newAppt.getStaff().getId());
        assertEquals(15, newAppt.getStartHour());
        assertEquals(AppointmentStatus.PENDING, newAppt.getStatus());
    }

    @Test
    @DisplayName("If first waiting patient has a conflict at slot hour, clinic skips to next waiting patient")
    void skipsWaitingPatientWithConflict() {
        Staff nurse = new Nurse(20, "Nurse Joy");
        clinic.registerStaff(nurse);

        // Sarah books doctor at hour 15
        Appointment appt1 = clinic.bookAppointment(patientSarah.getId(), doctor.getId(), 15);

        // John already has an appointment with Nurse at hour 15!
        clinic.bookAppointment(patientJohn.getId(), nurse.getId(), 15);

        // Both John and David join Dr. House's waiting list
        clinic.joinWaitingList(doctor.getId(), patientJohn.getId());
        clinic.joinWaitingList(doctor.getId(), patientDavid.getId());

        // Sarah cancels Appointment #1
        clinic.cancelAppointment(appt1.getId(), "Illness");

        // John was skipped because of conflict at hour 15, David receives the slot!
        List<Appointment> allAppts = clinic.getAppointments();
        Appointment davidAppt = allAppts.get(allAppts.size() - 1);

        assertEquals(patientDavid.getId(), davidAppt.getPatient().getId());
        assertEquals(doctor.getId(), davidAppt.getStaff().getId());
        assertEquals(15, davidAppt.getStartHour());
        assertEquals(AppointmentStatus.PENDING, davidAppt.getStatus());
    }
}
