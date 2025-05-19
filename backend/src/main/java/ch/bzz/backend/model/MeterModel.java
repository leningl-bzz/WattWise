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
    public String exportAllCSV() {
        StringBuilder sb = new StringBuilder();
        for (MeterData meter : allMeters.values()) {
            sb.append("Sensor: ").append(meter.getSensorId()).append("\n");
            sb.append(meter.exportCSV()).append("\n");
        }
        return sb.toString();
    }

    public String exportAllJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, MeterData> entry : allMeters.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            sb.append(entry.getValue().exportJSON());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}

