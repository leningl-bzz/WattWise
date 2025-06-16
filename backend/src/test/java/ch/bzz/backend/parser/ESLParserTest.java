package ch.bzz.backend.parser;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ESLParserTest {
    @Test
    void parseSample() {
        File file = new File("src/main/resources/testdata/esl-files/EdmRegisterWertExport_20220803_eslevu_20220803053522.xml");
        Map<String, Double> map = ESLParser.parseESLFile(file);
        assertEquals(24308.7, map.get("1-1:1.8.1"));
        assertEquals(42680.9, map.get("1-1:1.8.2"));
    }
}
