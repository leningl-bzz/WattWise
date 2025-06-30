package ch.bzz.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

    // Use @JsonIgnore if you don't want the raw TreeMap serialized,
    // as getAllMeasurements() will provide the list.
    // If you want the TreeMap structure, then remove @JsonIgnore and adjust frontend.
    // For the current frontend, getAllMeasurements() is preferred.
    @JsonIgnore
    public TreeMap<LocalDateTime, Measurement> getMeasurementsMap() {
        return measurements;
    }

    public Collection<Measurement> getMeasurements() {
        return measurements.values();
    }

    public String getSensorId() {
        return sensorId;
    }

    // Removed calculateAbsoluteValues() - it's handled by MeasurementMerger now.

    // Removed exportCSV() and exportJSON() as serialization is handled by Spring/Jackson
    // and CSV export is typically a separate endpoint or frontend concern.

    @Override
    public String toString() {
        return "MeterData{" +
                "sensorId='" + sensorId + '\'' +
                ", measurements=" + measurements.size() + " entries" +
                '}';
    }
}