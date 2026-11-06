package com.clinic.model;

/**
 * Interface representing academic lecturing responsibilities.
 *
 * Multiple Inheritance Restructuring Analysis:
 * In languages that support multiple class inheritance (such as C++), a class could
 * theoretically inherit from both Doctor and Lecturer classes. However, if both
 * Doctor and Lecturer were classes extending Person, this would produce the classic
 * "Diamond Problem" (deadly diamond of death), creating duplicate Person states
 * unless virtual inheritance is employed.
 *
 * In Java, multiple class inheritance is intentionally prohibited to eliminate diamond
 * ambiguities and state duplication. To model an entity that is both a Doctor and a Lecturer,
 * Lecturer is restructured as an interface defining academic capabilities, while Doctor remains
 * the primary single class inheritance chain (MedicalLecturer extends Doctor implements Lecturer).
 */
public interface Lecturer {

    /**
     * Returns the academic course or module taught by this lecturer.
     *
     * @return non-empty module name
     */
    String getModuleName();
}
