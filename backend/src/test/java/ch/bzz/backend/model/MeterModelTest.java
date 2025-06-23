package ch.bzz.backend.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MeterModelTest {

    @Test
    void testAddAndGetMeterData() {
        MeterModel model = new MeterModel();
        Measurement m = new Measurement(LocalDateTime.now(), 1.0, 2.0);
        model.addMeasurement("ID1", m);
        MeterData data = model.getMeterData("ID1");
        assertNotNull(data);
        assertTrue(data.getAllMeasurements().contains(m));
    }

    @Test
    void testExportAllCSV() {
        MeterModel model = new MeterModel();
        model.addMeasurement("ID2", new Measurement(LocalDateTime.of(2024,1,1,0,0), 1.0, 10.0));
        String csv = model.exportAllCSV();
        assertTrue(csv.contains("Sensor: ID2"));
        assertTrue(csv.contains("2024-01-01T00:00,1.0,10.0"));
    }

    @Test
    void testExportAllJSON() {
        MeterModel model = new MeterModel();
        model.addMeasurement("ID3", new Measurement(LocalDateTime.of(2024,1,1,0,0), 1.0, 10.0));
        String json = model.exportAllJSON();
        assertTrue(json.startsWith("{"));
        assertTrue(json.contains("\"ID3\":"));
        assertTrue(json.contains("\"timestamp\":\"2024-01-01T00:00\""));
    }
}
