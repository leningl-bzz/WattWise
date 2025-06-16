package ch.bzz.backend.parser;

import ch.bzz.backend.model.Measurement;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MeasurementMergerTest {
    @Test
    void mergeValues() {
        File sdat = new File("src/main/resources/testdata/sdat-files/20200214_093234_12X-0000001216-O_E66_12X-LIPPUNEREM-T_ESLEVU180263_-1809898866.xml");
        SDATParser.ParsedSDAT parsed = SDATParser.parseSDATFile(sdat);
        File esl = new File("src/main/resources/testdata/esl-files/EdmRegisterWertExport_20220803_eslevu_20220803053522.xml");
        Map<String, Double> map = ESLParser.parseESLFile(esl);
        List<Measurement> merged = MeasurementMerger.mergeWithESL(parsed.getValues(), map, "1-1:1.8.1", "1-1:1.8.2");
        assertEquals(parsed.getValues().size(), merged.size());
        assertEquals(66989.6, merged.get(0).getAbsolute());
    }
}
