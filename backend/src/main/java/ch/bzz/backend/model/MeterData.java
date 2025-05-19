package ch.bzz.backend.model;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.TreeMap;

/* Für jeden einzelnen Stromzähler */
public class MeterData {
    private String sensorId;  // z.B. ID735
    private TreeMap<LocalDateTime, Measurement> measurements;

    public MeterData(String sensorId) {
        this.sensorId = sensorId;
        this.measurements = new TreeMap<>();
    }

    public void addMeasurement(Measurement m) {
        this.measurements.put(m.getTimestamp(), m);
    }

    public Collection<Measurement> getAllMeasurements() {
        return measurements.values();
    }

    public String getSensorId() {
        return sensorId;
    }

    // Weitere Methoden: exportCSV(), exportJSON(), calculateAbsoluteValues() etc.
}

