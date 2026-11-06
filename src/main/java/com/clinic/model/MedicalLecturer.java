package com.clinic.model;

import com.clinic.exception.ValidationException;

/**
 * =========================================================================
 * INVARIANTS FOR MedicalLecturer:
 * 1. Exactly one Person instance is constructed per MedicalLecturer object.
 * 2. Role is strictly defined as "Doctor and Lecturer".
 * 3. Module name must be non-null and non-blank.
 * 4. Consultation fee returns the clinical Doctor fee (super.fee()).
 * =========================================================================
 *
 * Models a practitioner who is both a practicing medical doctor and a university lecturer.
 */
public class MedicalLecturer extends Doctor implements Lecturer {

    private final String moduleName;

    /**
     * Constructs a MedicalLecturer.
     * Invokes Doctor -> Staff -> Person constructor chain exactly once.
     *
     * @param id unique positive identifier
     * @param name non-empty full name
     * @param moduleName non-empty academic module name
     * @throws ValidationException if moduleName is null or empty
     */
    public MedicalLecturer(int id, String name, String moduleName) {
        super(id, name);
        if (moduleName == null || moduleName.trim().isEmpty()) {
            throw new ValidationException("Lecturer module name must not be null or blank.");
        }
        this.moduleName = moduleName.trim();
    }

    /**
     * Constructs a MedicalLecturer with a custom university parameter P for fee testing.
     *
     * @param id unique positive identifier
     * @param name non-empty full name
     * @param moduleName non-empty academic module name
     * @param customP parameter P
     */
    public MedicalLecturer(int id, String name, String moduleName, int customP) {
        super(id, name, customP);
        if (moduleName == null || moduleName.trim().isEmpty()) {
            throw new ValidationException("Lecturer module name must not be null or blank.");
        }
        this.moduleName = moduleName.trim();
    }

    /**
     * MedicalLecturer role combining both medical and academic titles.
     *
     * @return exactly "Doctor and Lecturer"
     */
    @Override
    public String role() {
        return "Doctor and Lecturer";
    }

    /**
     * Academic module name.
     *
     * @return course module name
     */
    @Override
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Rationale for MedicalLecturer.fee():
     * Returns super.fee() (the standard Doctor consultation fee).
     *
     * In a clinical setting, patients schedule appointments for diagnosis, treatment,
     * and medical care. The clinic charges for medical service delivery, which is governed
     * by the practitioner's licensed medical qualification (Doctor).
     * The teaching workload is compensated separately through university academic payroll,
     * not billed to clinic patients during consultation.
     *
     * @return doctor consultation fee
     */
    @Override
    public double fee() {
        return super.fee();
    }

    @Override
    public String toString() {
        return String.format("[%s #%d] %s (Module: %s)", role(), getId(), getName(), moduleName);
    }
}
