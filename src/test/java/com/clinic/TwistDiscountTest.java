package com.clinic;

import com.clinic.clock.FixedClock;
import com.clinic.config.ClinicConfig;
import com.clinic.config.TwistMode;
import com.clinic.model.*;
import com.clinic.service.Clinic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Twist 3: Patient Discounts Polymorphism Tests")
class TwistDiscountTest {

    @AfterEach
    void tearDown() {
        ClinicConfig.resetToDefaults();
    }

    @Test
    @DisplayName("Patient discounts: student 50%, staff-member 0%, visitor 100% applied polymorphically")
    void discountsAppliedPolymorphically() {
        ClinicConfig.setP(0);
        Staff doctor = new Doctor(1, "Dr. House"); // Fee: 40.0
        Staff nurse = new Nurse(2, "Nurse Joy");   // Fee: 10.0
        Staff pharm = new Pharmacist(3, "Bob");    // Fee: 5.0

        Patient visitor = new VisitorPatient(10, "Visitor Vic");
        Patient student = new StudentPatient(20, "Student Sam");
        Patient staffPatient = new StaffPatient(30, "Staff Sally");

        // Doctor appointment ($40.00 base)
        assertEquals(40.00, visitor.calculateCharge(doctor.fee()), 0.001);
        assertEquals(20.00, student.calculateCharge(doctor.fee()), 0.001); // 50%
        assertEquals(0.00, staffPatient.calculateCharge(doctor.fee()), 0.001); // 0%

        // Nurse appointment ($10.00 base)
        assertEquals(10.00, visitor.calculateCharge(nurse.fee()), 0.001);
        assertEquals(5.00, student.calculateCharge(nurse.fee()), 0.001); // 50%
        assertEquals(0.00, staffPatient.calculateCharge(nurse.fee()), 0.001); // 0%

        // Pharmacist appointment ($5.00 base)
        assertEquals(5.00, visitor.calculateCharge(pharm.fee()), 0.001);
        assertEquals(2.50, student.calculateCharge(pharm.fee()), 0.001); // 50%
        assertEquals(0.00, staffPatient.calculateCharge(pharm.fee()), 0.001); // 0%
    }

    @Test
    @DisplayName("Appointment.calculatePrice() delegates polymorphically to patient and staff")
    void appointmentCalculatesPricePolymorphically() {
        ClinicConfig.setP(0);
        Staff doctor = new Doctor(1, "Dr. House");
        Patient student = new StudentPatient(2, "Student Sam");

        Appointment appt = new Appointment(100, student, doctor, 15, new FixedClock(10));
        assertEquals(20.00, appt.calculatePrice(), 0.001);
    }

    @Test
    @DisplayName("Clinic twist configuration controls whether appointment discounts apply")
    void discountModeIsConfigurable() {
        Patient student = new StudentPatient(1, "Student Sam");
        Staff doctor = new Doctor(2, "Dr. House");

        Clinic standardClinic = new Clinic(new FixedClock(10), TwistMode.STANDARD);
        standardClinic.registerPatient(student);
        standardClinic.registerStaff(doctor);
        Appointment fullPrice = standardClinic.bookAppointment(1, 2, 15);
        assertEquals(40.0, fullPrice.calculatePrice(), 0.001);

        Clinic discountClinic = new Clinic(new FixedClock(10), TwistMode.TWIST_3_DISCOUNTS);
        Patient discountedStudent = new StudentPatient(3, "Student Lee");
        Staff anotherDoctor = new Doctor(4, "Dr. Jones");
        discountClinic.registerPatient(discountedStudent);
        discountClinic.registerStaff(anotherDoctor);
        Appointment discounted = discountClinic.bookAppointment(3, 4, 15);
        assertEquals(20.0, discounted.calculatePrice(), 0.001);
    }
}
