package ch.bzz.backend.parser;

import ch.bzz.backend.model.Measurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class SDATParser {

    private static final Logger logger = LoggerFactory.getLogger(SDATParser.class);
    private static final String STROM_NAMESPACE = "http://www.strom.ch";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;


    public static ParsedSDAT parseSDATFile(File file) {
        List<Measurement> measurements = new ArrayList<>();
        String documentId = null;

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true); // VERY IMPORTANT FOR NAMESPACES
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);
            doc.getDocumentElement().normalize();

            // Extract DocumentID
            NodeList docIdNodes = doc.getElementsByTagNameNS(STROM_NAMESPACE, "DocumentID");
            if (docIdNodes.getLength() > 0) {
                // Check if it's the correct DocumentID within InstanceDocument
                Element docIdElement = (Element) docIdNodes.item(0);
                // This is a more robust way to get the *first* DocumentID that's under InstanceDocument
                Node parentNode = docIdElement.getParentNode();
                if (parentNode != null && parentNode.getNodeName().equals("rsm:InstanceDocument")) {
                    documentId = docIdElement.getTextContent().trim();
                    logger.info("SDATParser: Found DocumentID: {}", documentId);
                } else {
                    logger.warn("SDATParser: DocumentID found, but not under rsm:InstanceDocument. Skipping.");
                }
            } else {
                logger.warn("SDATParser: No DocumentID found in the XML.");
            }


            // Extract Observations
            NodeList observationNodes = doc.getElementsByTagNameNS(STROM_NAMESPACE, "Observation");
            logger.info("SDATParser: Found {} 'Observation' nodes.", observationNodes.getLength()); // <<< ADD THIS LOG

            // You need to get the StartDateTime of the Interval, not per observation
            LocalDateTime intervalStart = null;
            NodeList intervalStartNodes = doc.getElementsByTagNameNS(STROM_NAMESPACE, "StartDateTime");
            if (intervalStartNodes.getLength() > 0) {
                for (int i = 0; i < intervalStartNodes.getLength(); i++) {
                    Node node = intervalStartNodes.item(i);
                    if (node.getParentNode() != null && node.getParentNode().getNodeName().equals("rsm:Interval")) {
                        try {
                            intervalStart = LocalDateTime.parse(node.getTextContent().trim(), DATE_TIME_FORMATTER);
                            break; // Take the first one found within an Interval
                        } catch (DateTimeParseException e) {
                            logger.warn("SDATParser: Could not parse Interval StartDateTime: {}", node.getTextContent(), e);
                        }
                    }
                }
            }
            if (intervalStart == null) {
                logger.error("SDATParser: No valid Interval StartDateTime found. Cannot calculate timestamps for observations.");
                return new ParsedSDAT(documentId, new ArrayList<>()); // Return empty if critical data is missing
            }

            // Extract Resolution
            int resolutionMinutes = 15; // Default as per your file example
            NodeList resolutionNodes = doc.getElementsByTagNameNS(STROM_NAMESPACE, "Resolution");
            if (resolutionNodes.getLength() > 0) {
                NodeList resSubNodes = ((Element) resolutionNodes.item(0)).getElementsByTagNameNS(STROM_NAMESPACE, "Resolution");
                if (resSubNodes.getLength() > 0) {
                    try {
                        resolutionMinutes = Integer.parseInt(resSubNodes.item(0).getTextContent().trim());
                    } catch (NumberFormatException e) {
                        logger.warn("SDATParser: Could not parse Resolution value. Defaulting to 15 minutes.", e);
                    }
                }
            }


            for (int i = 0; i < observationNodes.getLength(); i++) {
                Node observationNode = observationNodes.item(i);
                if (observationNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element observationElement = (Element) observationNode;

                    // Get Volume
                    NodeList volumeNodes = observationElement.getElementsByTagNameNS(STROM_NAMESPACE, "Volume");
                    double relativeVolume = 0.0;
                    if (volumeNodes.getLength() > 0) {
                        try {
                            relativeVolume = Double.parseDouble(volumeNodes.item(0).getTextContent().trim());
                        } catch (NumberFormatException e) {
                            logger.warn("SDATParser: Could not parse Volume for observation {}: {}", i + 1, volumeNodes.item(0).getTextContent(), e);
                        }
                    } else {
                        logger.warn("SDATParser: Volume tag not found for observation {}", i + 1); // <<< ADD THIS LOG
                    }

                    // Calculate timestamp based on interval start and sequence
                    // The Sequence number is 1-based, so subtract 1 for array index
                    NodeList sequenceNodes = observationElement.getElementsByTagNameNS(STROM_NAMESPACE, "Sequence");
                    int sequence = 0;
                    if (sequenceNodes.getLength() > 0) {
                        try {
                            sequence = Integer.parseInt(sequenceNodes.item(0).getTextContent().trim());
                        } catch (NumberFormatException e) {
                            logger.warn("SDATParser: Could not parse Sequence for observation {}: {}", i + 1, sequenceNodes.item(0).getTextContent(), e);
                        }
                    } else {
                        logger.warn("SDATParser: Sequence tag not found for observation {}", i + 1);
                    }

                    LocalDateTime currentTimestamp = intervalStart.plusMinutes((long) (sequence - 1) * resolutionMinutes);


                    measurements.add(new Measurement(currentTimestamp, relativeVolume, null)); // absolute will be calculated by merger
                }
            }
            return new ParsedSDAT(documentId, measurements);

        } catch (ParserConfigurationException | SAXException | IOException e) {
            logger.error("Error parsing SDAT file {}: {}", file.getName(), e.getMessage(), e);
            return new ParsedSDAT(documentId, new ArrayList<>()); // Return empty list on error
        }
    }

    // Inner class for returning parsed data
    public static class ParsedSDAT {
        private String documentId;
        private List<Measurement> values;

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
}