package ch.bzz.backend.parser;

import ch.bzz.backend.model.Measurement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MeasurementMerger {

    public static List<Measurement> mergeWithESL(List<Measurement> sdatValues, Map<String, Double> eslMap, String obis1, String obis2) {
        double startValue = eslMap.getOrDefault(obis1, 0.0) + eslMap.getOrDefault(obis2, 0.0);
        double total = startValue;
        List<Measurement> result = new ArrayList<>();

        for (Measurement m : sdatValues) {
            total += m.getRelative();
            result.add(new Measurement(m.getTimestamp(), m.getRelative(), total));
        }

        return result;
    }
}

