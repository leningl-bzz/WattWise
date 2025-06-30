package ch.bzz.backend.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MeterDataTest {

    @Test
    void testAddAndGetAllMeasurements() {
        MeterData meter = new MeterData("ID1");
        Measurement m1 = new Measurement(LocalDateTime.now(), 1.0, null);
        meter.addMeasurement(m1);
        assertTrue(meter.getMeasurements().contains(m1));
    }

    @Test
    void testCalculateAbsoluteValues() {
        MeterData meter = new MeterData("ID2");
        meter.addMeasurement(new Measurement(LocalDateTime.of(2024,1,1,0,0), 1.0, null));
        meter.addMeasurement(new Measurement(LocalDateTime.of(2024,1,1,1,0), 2.0, null));
        meter.calculateAbsoluteValues(10.0);
        List<Measurement> list = meter.getMeasurements().stream().toList();
        assertEquals(11.0, list.get(0).getAbsoluteValue());
        assertEquals(13.0, list.get(1).getAbsoluteValue());
    }

    @Test
    void testExportCSV() {
        MeterData meter = new MeterData("ID3");
        meter.addMeasurement(new Measurement(LocalDateTime.of(2024,1,1,0,0), 1.0, 11.0));
        String csv = meter.exportCSV();
        assertTrue(csv.contains("Timestamp,Relative Value,Absolute Value"));
        assertTrue(csv.contains("2024-01-01T00:00,1.0,11.0"));
    }

    @Test
    void testExportJSON() {
        MeterData meter = new MeterData("ID4");
        meter.addMeasurement(new Measurement(LocalDateTime.of(2024,1,1,0,0), 1.0, 11.0));
        String json = meter.exportJSON();
        assertTrue(json.startsWith("["));
        assertTrue(json.contains("\"timestamp\":\"2024-01-01T00:00\""));
        assertTrue(json.contains("\"relativeValue\":1.0"));
        assertTrue(json.contains("\"absoluteValue\":11.0"));
    }
}
