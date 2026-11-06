package com.clinic;

import com.clinic.clock.Clock;
import com.clinic.clock.FixedClock;
import com.clinic.clock.SimulatedClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Clock Behaviour and Isolation Tests")
class ClockBehaviourTest {

    @Test
    @DisplayName("FixedClock returns constant snapshot hour")
    void fixedClockReturnsConstantHour() {
        Clock fixed = new FixedClock(42);
        assertEquals(42, fixed.now());
        assertEquals(42, fixed.now());
    }

    @Test
    @DisplayName("SimulatedClock accurately advances and enforces weekly boundaries [0, 168]")
    void simulatedClockAdvancesAndEnforcesBounds() {
        SimulatedClock clock = new SimulatedClock(0);
        assertEquals(0, clock.now());

        clock.advance(5);
        assertEquals(5, clock.now());

        clock.setTime(168);
        assertEquals(168, clock.now());

        // Boundary violations: must not exceed 168 or be negative
        assertThrows(IllegalArgumentException.class, () -> clock.setTime(-1));
        assertThrows(IllegalArgumentException.class, () -> clock.setTime(169));
        assertThrows(IllegalArgumentException.class, () -> clock.advance(1)); // 168 + 1 > 168
        assertThrows(IllegalArgumentException.class, () -> clock.advance(-5));
    }
}
