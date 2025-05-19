package ch.bzz.backend.runner;

import ch.bzz.backend.model.Messwert;
import ch.bzz.backend.parser.ESLParser;
import ch.bzz.backend.parser.MesswertMerger;
import ch.bzz.backend.parser.SDATParser;

import java.io.File;

import java.util.List;
import java.util.Map;

public class ParserRunner {

    public static void main(String[] args) {
        try {
            // === 1. ESL-Datei einlesen ===
            File eslFile = new File("data/ESL.xml"); // Pfad anpassen!
            Map<String, Double> eslWerte = ESLParser.parseESLFile(eslFile);

            // === 2. SDAT-Datei einlesen ===
            File sdatFile = new File("data/SDAT.xml"); // Pfad anpassen!
            SDATParser.ParsedSDAT parsed = SDATParser.parseSDATFile(sdatFile);
            List<Messwert> sdatWerte = parsed.getWerte();
            String documentId = parsed.getDocumentId();

            // === 3. OBIS-Codes bestimmen anhand der DocumentID ===
            String obis1, obis2;
            if (documentId.contains("ID742")) {
                obis1 = "1-1:1.8.1"; // Bezug HT
                obis2 = "1-1:1.8.2"; // Bezug NT
            } else if (documentId.contains("ID735")) {
                obis1 = "1-1:2.8.1"; // Einspeisung HT
                obis2 = "1-1:2.8.2"; // Einspeisung NT
            } else {
                System.out.println("Unbekannte DocumentID: " + documentId);
                return;
            }

            // === 4. Verknüpfung & Ausgabe ===
            List<Messwert> result = MesswertMerger.verknuepfenMitESL(sdatWerte, eslWerte, obis1, obis2);
            result.forEach(System.out::println);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
