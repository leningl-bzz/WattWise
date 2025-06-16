package ch.bzz.backend.parser;

import ch.bzz.backend.model.Measurement;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SDATParserTest {
    @Test
    void parseSample() {
        File file = new File("src/main/resources/testdata/sdat-files/20200214_093234_12X-0000001216-O_E66_12X-LIPPUNEREM-T_ESLEVU180263_-1809898866.xml");
        SDATParser.ParsedSDAT parsed = SDATParser.parseSDATFile(file);
        assertEquals("eslevu180263_BR2294_ID735", parsed.getDocumentId());
        List<Measurement> list = parsed.getValues();
        assertEquals(96, list.size());
        assertEquals(0.0, list.get(0).getRelative());
    }
}
