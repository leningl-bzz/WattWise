package ch.bzz.backend.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class MeterDataTest {

    @Test
    void testAddAndGetAllMeasurements() {
        MeterData meter = new MeterData("ID1");
        Measurement m1 = new Measurement(LocalDateTime.now(), 1.0, 1.0);
        meter.addMeasurement(m1);
        
        Collection<Measurement> measurements = meter.getMeasurements();
        assertTrue(measurements.contains(m1));
        assertEquals(1, measurements.size());
    }

    @Test
    void testGetSensorId() {
        String sensorId = "ID2";
        MeterData meter = new MeterData(sensorId);
        assertEquals(sensorId, meter.getSensorId());
    }
}
