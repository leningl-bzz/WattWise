package ch.bzz.backend.parser;

import ch.bzz.backend.model.Measurement;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.time.LocalDateTime;
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

            NodeList docIdNodes = doc.getElementsByTagNameNS("http://www.strom.ch", "DocumentID");
            if (docIdNodes.getLength() > 0) {
                documentId = docIdNodes.item(0).getTextContent().trim();
            }

            NodeList startNodes = doc.getElementsByTagNameNS("http://www.strom.ch", "StartDateTime");
            if (startNodes.getLength() == 0) return new ParsedSDAT(documentId, measurements);
            LocalDateTime startTime = LocalDateTime.parse(startNodes.item(0).getTextContent().replace("Z", ""));

            int minutes = 15;
            NodeList resNodes = doc.getElementsByTagNameNS("http://www.strom.ch", "Resolution");
            for (int i = 0; i < resNodes.getLength(); i++) {
                Node n = resNodes.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element) n;
                    NodeList resInner = el.getElementsByTagNameNS("http://www.strom.ch", "Resolution");
                    if (resInner.getLength() > 0) {
                        minutes = Integer.parseInt(resInner.item(0).getTextContent());
                        break;
                    }
                }
            }

            NodeList obsNodes = doc.getElementsByTagNameNS("http://www.strom.ch", "Observation");
            for (int i = 0; i < obsNodes.getLength(); i++) {
                Element obs = (Element) obsNodes.item(i);
                String volStr = obs.getElementsByTagNameNS("http://www.strom.ch", "Volume").item(0).getTextContent();
                double relative = Double.parseDouble(volStr);
                LocalDateTime ts = startTime.plusMinutes(i * minutes);
                measurements.add(new Measurement(ts, relative, null));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ParsedSDAT(documentId, measurements);
    }
}
