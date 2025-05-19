package ch.bzz.backend.model;

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

    public Collection<MeterData> getAllMeterData() {
        return allMeters.values();
    }

    // JSON/CSV Export Methoden hier
}

