package ch.bzz.backend.runner;

import ch.bzz.backend.model.Measurement;
import ch.bzz.backend.parser.ESLParser;
import ch.bzz.backend.parser.MeasurementMerger;
import ch.bzz.backend.parser.SDATParser;

import java.io.File;

import java.util.List;
import java.util.Map;

public class ParserRunner {

    public static void main(String[] args) {
        try {
            File eslFile = new File("data/ESL.xml");
            File sdatFile = new File("data/SDAT.xml");

            Map<String, Double> eslValues = ESLParser.parseESLFile(eslFile);
            SDATParser.ParsedSDAT parsed = SDATParser.parseSDATFile(sdatFile);
            List<Measurement> sdatMeasurements = parsed.getValues();
            String documentId = parsed.getDocumentId();

            String obis1, obis2;
            if (documentId.contains("ID742")) {
                obis1 = "1-1:1.8.1";
                obis2 = "1-1:1.8.2";
            } else if (documentId.contains("ID735")) {
                obis1 = "1-1:2.8.1";
                obis2 = "1-1:2.8.2";
            } else {
                System.out.println("Unknown DocumentID: " + documentId);
                return;
            }

            List<Measurement> result = MeasurementMerger.mergeWithESL(sdatMeasurements, eslValues, obis1, obis2);
            result.forEach(System.out::println);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
