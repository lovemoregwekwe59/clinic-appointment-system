# Design Decision Log (DECISIONS.md)

This document provides in-depth justifications for the architectural and object-oriented design decisions made in the Clinic Appointment System, addressing questions **D1** through **D6** as specified in the assignment requirements.

---

### D1. Where does the 2-hour rule live (Appointment, Clinic, Staff, or a separate policy object)? What other places did you consider, and what is the trade-off?

* **Selected Location:** Inside `Appointment.cancel(String reason)`. The appointment keeps the injected `Clock` and reads `clock.now()` when validating the transition.
* **Alternatives Considered:**
  1. *Inside `Clinic`:* Placing the 2-hour calculation in `Clinic` was considered. This centralizes coordination but makes direct lifecycle changes harder to protect and risks scattering rules across service methods.
  2. *Inside `Staff`:* Considered whether the staff member should dictate cancellation notice. However, the 2-hour cancellation rule is a universal clinic policy applicable across all confirmed appointments, not a clinician-specific preference.
  3. *Inside a separate `CancellationPolicy` strategy object:* Considered for dynamic configurable rule engines. While flexible, for this system's scope, a separate class introduces unnecessary indirection without tangible benefit.
* **Trade-off and Justification:**
  Following the **Information Expert** GRASP pattern, `Appointment` holds its own `status` and `startHour`. `Appointment` enforces its own class invariants and protects its state machine from illegal transitions. Clinic uses a non-mutating validation before logging success, then asks the appointment to perform the transition.

---

### D2. Why is your clock design testable, and what could not be tested if you had called the system time directly?

* **Selected Design:** Injected `Clock` interface (`com.clinic.clock.Clock`) with implementations `SimulatedClock` and `FixedClock`.
* **Why Testable:**
  Unit tests require strict determinism, reproducibility, and isolation. With `FixedClock`, tests can freeze clinic time at any desired hour (e.g., hour 10 or hour 14) and verify state transitions instantly without wall-clock delays. With `SimulatedClock`, tests can advance time by whole hours to test weekly boundaries and lifecycle progression.
* **What could NOT be tested if `System.currentTimeMillis()` or `Instant.now()` were used:**
  1. *Real-time delays:* Testing a rule that requires a 2-hour window would otherwise need real delays or time mocking around system calls.
  2. *Non-determinism:* Tests could behave differently depending on when and where they run.
  3. *Weekly bounds (0-168):* Synthetic simulation of whole-week boundaries could not be reliably tested using wall-clock time.

---

### D3. When you added Pharmacist, what did you change besides the new class? If anything else, say what leaked?

* **Selected Design:** Created `Pharmacist extends Staff` implementing `role()` -> `"Pharmacist"`, `fee()` -> `5.0`, and `bufferHours()` -> `0`.
* **What Changed Besides the New Class:**
  * **Core Domain:** **NOTHING.** Zero lines of code were modified in `Clinic`, `Appointment`, `Person`, `Staff`, or the polymorphic fee printing loop.
  * **User Interface:** A single line was added to the console menu choice list (`handleRegisterStaff`) so that console users can select "3. Pharmacist" when registering a practitioner.
* **Did anything leak?**
  No domain logic leaked. The system strictly adheres to the **Open-Closed Principle (OCP)**: open for extension (adding `Pharmacist`), closed for modification (existing billing, conflict detection, and scheduling algorithms remained untouched).

---

### D4. What should MedicalLecturer.fee() return, and which language feature stops Person being created twice? What would have happened without it?

* **Selected Fee Behavior:** `MedicalLecturer.fee()` returns `super.fee()` (the standard Doctor consultation fee).
* **Rationale:**
  In a clinical scheduling context, when a patient books an appointment, they are receiving medical diagnosis and treatment from a licensed physician. The patient is charged for healthcare delivery. The practitioner's academic role (teaching university modules) is compensated by the university through faculty payroll, not billed as an additional out-of-pocket medical surcharge to clinic patients.
* **Which language feature stops `Person` being created twice?**
  Java's **single class inheritance model combined with interfaces**. In Java, a class can extend at most one superclass. `MedicalLecturer` extends `Doctor` (which extends `Staff`, which extends `Person`) and implements the `Lecturer` interface.
* **What would have happened without it (Multiple Inheritance Diamond Problem)?**
  In languages supporting multiple class inheritance without virtual base classes (such as C++ without `virtual public Person`), if `Doctor` extended `Person` and `Lecturer` also extended `Person`, `MedicalLecturer` would inherit **two separate copies of `Person`** in memory. This causes:
  1. Ambiguity when querying `getId()` or `getName()` (compiler cannot determine which subobject to access).
  2. Inconsistent state (one `Person` subobject could have its name updated while the other remained unchanged).
  3. Memory waste and constructor duplication.
  Our unit test `MedicalLecturerMultipleInheritanceTest` verifies via `Person.getConstructorCallCount()` that `Person`'s constructor is invoked exactly **once**.

---

### D5. What should happen if writing to or closing the log itself fails?

* **Selected Mechanism:** Java's **`try-with-resources`** construct managing `AppointmentLog implements AutoCloseable`.
* **Behavior Analysis:**
  1. *Normal operation:* Log opens (prints `[log] opened`), operation succeeds, log closes (prints `[log] closed`).
  2. *Operation fails, close succeeds:* Clinic attempts to write `FAILED cancel #3: ...`, then try-with-resources invokes `close()` once. If logging works, the original domain error propagates.
  3. *Business-action write fails:* Clinic writes success before committing its in-memory change, so a failed success write prevents that change. If writing the failure record also fails, that `LoggingException` is attached to the original error as suppressed; the original error is still propagated.
  4. *Close fails:* The close marker is attempted once and a `LoggingException` is surfaced. If a business error is already in flight, Java's try-with-resources attaches the close error as a suppressed exception. If the operation had already committed after a successful log write, the caller is still told that close failed.
  5. *Unavoidable limit:* If the output device itself is failing, the system cannot promise that a marker reaches that device. It reports the failure and never claims the physical log succeeded.

---

### D6. Twist Decisions and Rationale

#### (0) Who owns the waiting list and why?
* **Owner:** The `Clinic` service manages `Map<Integer, List<Patient>> staffWaitingLists`.
* **Why:** Waiting list allocation requires clinic-wide authority. When an appointment is cancelled, the clinic must check whether candidate patients have conflicting appointments with *other* staff members at that same hour. Individual `Staff` objects do not have access to other practitioners' schedules. Centralizing the queue in `Clinic` maintains encapsulation while allowing global conflict resolution.

#### (1) Why can't a confirmed appointment be bumped?
* **Rationale:** A `CONFIRMED` appointment represents a finalized clinical pact between patient and provider. If confirmed appointments could be unilaterally cancelled by emergency arrivals, patient scheduling reliability would be destroyed. Emergency bumping is strictly restricted to provisional `PENDING` bookings.

#### (2) Why `bufferHours()` and not a type check?
* **Rationale:** Runtime type inspection violates the Open-Closed Principle and degrades maintainability. By declaring `public abstract int bufferHours()` on `Staff`, each staff subclass encapsulates its own operational buffer (`Doctor` = 2, `Nurse` = 1, `Pharmacist` = 0). The clinic queries the buffer polymorphically without needing to know the concrete runtime class.

#### (3) Where does the discount live and why?
* **Location:** Inside the `Patient` class hierarchy (`Patient.calculateCharge(baseFee)` and `discountMultiplier()`), overridden by `StudentPatient` (50%), `StaffPatient` (0%), and `VisitorPatient` (100%).
* **Why:** Discount eligibility is a fundamental characteristic of the *patient's relationship with the university/clinic*. Computing the discounted charge on `Patient` allows polymorphism to resolve the final price. `Appointment` captures whether the configured booking mode enables that discount; it does not inspect patient subtypes.
