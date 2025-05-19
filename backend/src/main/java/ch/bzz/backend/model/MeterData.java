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
    
    public void calculateAbsoluteValues(double initialValue) {
        double sum = initialValue;
        for (Measurement m : measurements.values()) {
            sum += m.getRelativeValue();
            m.setAbsoluteValue(sum);
        }
    }

    public String exportCSV() {
        StringBuilder sb = new StringBuilder();
        sb.append("Timestamp,Relative Value,Absolute Value\n");
        for (Measurement m : measurements.values()) {
            sb.append(m.getTimestamp()).append(",");
            sb.append(m.getRelativeValue()).append(",");
            sb.append(m.getAbsoluteValue() != null ? m.getAbsoluteValue() : "").append("\n");
        }
        return sb.toString();
    }

    public String exportJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (Measurement m : measurements.values()) {
            if (!first) sb.append(",");
            sb.append("{");
            sb.append("\"timestamp\":\"").append(m.getTimestamp()).append("\",");
            sb.append("\"relativeValue\":").append(m.getRelativeValue()).append(",");
            sb.append("\"absoluteValue\":").append(m.getAbsoluteValue() != null ? m.getAbsoluteValue() : "null");
            sb.append("}");
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }
}

