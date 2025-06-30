package ch.bzz.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/* Daten für mehrere Zähler */
public class MeterModel {
    private Map<String, MeterData> allMeters;  // key = sensorId (z.B. ID735)

    public MeterModel() {
        allMeters = new HashMap<>();
    }

    public void addMeasurement(String sensorId, Measurement measurement) {
        allMeters.computeIfAbsent(sensorId, id -> new MeterData(id)).addMeasurement(measurement);
    }

    public MeterData getMeterData(String sensorId) {
        return allMeters.get(sensorId);
    }

    // This getter (getAllMeterData) will be serialized by Jackson to "allMeterData",
    // which matches your frontend's MeterModelResponse type.
    public Collection<MeterData> getAllMeterData() {
        return allMeters.values();
    }

    // Optional: If you explicitly want the map itself exposed in JSON, remove @JsonIgnore.
    // For frontend to work, 'allMeterData' is sufficient.
    @JsonIgnore
    public Map<String, MeterData> getAllMetersMap() {
        return allMeters;
    }

    // Removed exportAllCSV() and exportAllJSON() - Jackson handles JSON serialization.
    // CSV export is typically a separate endpoint or frontend responsibility.
}