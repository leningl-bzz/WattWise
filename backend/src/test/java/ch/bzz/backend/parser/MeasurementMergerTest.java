package ch.bzz.backend.parser;

import ch.bzz.backend.model.Measurement;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MeasurementMergerTest {

    @Test
    void testMergeWithESL_EmptyMap() {
        List<Measurement> sdat = List.of(
                new Measurement(LocalDateTime.of(2024, 1, 1, 0, 0), 1.0, null)
        );
        Map<String, Double> eslMap = new HashMap<>();
        List<Measurement> merged = MeasurementMerger.mergeWithESL(sdat, eslMap, "X", "Y");
        assertEquals(1, merged.size());
        assertEquals(1.0, merged.get(0).getAbsoluteValue());
    }
}
