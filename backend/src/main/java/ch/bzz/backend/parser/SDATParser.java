package ch.bzz.backend.parser;

import ch.bzz.backend.model.Measurement;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SDATParser {

    public static class ParsedSDAT {
        private final String documentId;
        private final List<Measurement> values;

        public ParsedSDAT(String documentId, List<Measurement> values) {
            this.documentId = documentId;
            this.values = values;
        }

        public String getDocumentId() {
            return documentId;
        }

        public List<Measurement> getValues() {
            return values;
        }
    }

    public static ParsedSDAT parseSDATFile(File xmlFile) {
        List<Measurement> measurements = new ArrayList<>();
        String documentId = null;

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // Get Document ID
            NodeList docIdNodes = doc.getElementsByTagNameNS("*", "DocumentID");
            if (docIdNodes.getLength() > 0) {
                documentId = docIdNodes.item(0).getTextContent().trim();
            }
            System.out.println("Document ID: " + documentId);

            // Get Start Time
            NodeList startNodes = doc.getElementsByTagNameNS("*", "StartDateTime");
            if (startNodes.getLength() == 0) {
                System.out.println("Kein StartDateTime gefunden.");
                return new ParsedSDAT(documentId, measurements);
            }
            String startStr = startNodes.item(0).getTextContent().replace("Z", "");
            LocalDateTime startTime = LocalDateTime.parse(startStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            System.out.println("Startzeit: " + startTime);

            // Default: 15 Minuten Intervalle
            int minutes = 15;
            NodeList resolutionNodes = doc.getElementsByTagNameNS("*", "Resolution");
            if (resolutionNodes.getLength() > 0) {
                try {
                    minutes = Integer.parseInt(resolutionNodes.item(0).getTextContent());
                } catch (NumberFormatException ignored) {
                }
            }
            System.out.println("Intervall: " + minutes + " Minuten");

            // Read Observations
            NodeList obsNodes = doc.getElementsByTagNameNS("*", "Observation");
            System.out.println("Anzahl Observation-Knoten: " + obsNodes.getLength());

            for (int i = 0; i < obsNodes.getLength(); i++) {
                Node node = obsNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element obs = (Element) node;
                    NodeList volumeNodes = obs.getElementsByTagNameNS("*", "Volume");
                    if (volumeNodes.getLength() > 0) {
                        String volStr = volumeNodes.item(0).getTextContent();
                        try {
                            double relative = Double.parseDouble(volStr);
                            LocalDateTime timestamp = startTime.plusMinutes(i * minutes);
                            System.out.println("Timestamp: " + timestamp + ", relative: " + relative);
                            measurements.add(new Measurement(timestamp, relative, null));
                        } catch (NumberFormatException e) {
                            System.err.println("Ungültiger Wert in Volume: " + volStr);
                        }
                    } else {
                        System.out.println("Keine Volume-Tags gefunden in Observation #" + i);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ParsedSDAT(documentId, measurements);
    }
}
