package ch.bzz.backend.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MeasurementTest {

    @Test
    void testConstructorAndGetters() {
        LocalDateTime now = LocalDateTime.now();
        Measurement m = new Measurement(now, 1.5, 10.0);
        assertEquals(now, m.getTimestamp());
        assertEquals(1.5, m.getRelative());
        assertEquals(10.0, m.getAbsolute());
    }

    @Test
    void testSetAbsolute() {
        Measurement m = new Measurement(LocalDateTime.now(), 2.0, 5.0);
        m.setAbsolute(8.0);
        assertEquals(8.0, m.getAbsolute());
    }

    @Test
    void testRelativeAndAbsoluteValueMethods() {
        Measurement m = new Measurement(LocalDateTime.now(), 3.0, 7.0);
        assertEquals(3.0, m.getRelative());
        assertEquals(7.0, m.getAbsolute());
        m.setAbsolute(9.0);
        assertEquals(9.0, m.getAbsolute());
    }

    @Test
    void testToString() {
        Measurement m = new Measurement(LocalDateTime.of(2024, 1, 1, 12, 0), 1.0, 2.0);
        String s = m.toString();
        assertTrue(s.contains("timestamp="));
        assertTrue(s.contains("relative=1.0"));
        assertTrue(s.contains("absolute=2.0"));
    }
}
