package ch.bzz.backend.parser;

import ch.bzz.backend.model.Messwert;
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
        private final List<Messwert> werte;

        public ParsedSDAT(String documentId, List<Messwert> werte) {
            this.documentId = documentId;
            this.werte = werte;
        }

        public String getDocumentId() {
            return documentId;
        }

        public List<Messwert> getWerte() {
            return werte;
        }
    }

    public static ParsedSDAT parseSDATFile(File xmlFile) {
        List<Messwert> messwerte = new ArrayList<>();
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
            if (startNodes.getLength() == 0) return new ParsedSDAT(documentId, messwerte);
            LocalDateTime startTime = LocalDateTime.parse(startNodes.item(0).getTextContent().replace("Z", ""));

            int minuten = 15;
            NodeList resNodes = doc.getElementsByTagNameNS("http://www.strom.ch", "Resolution");
            for (int i = 0; i < resNodes.getLength(); i++) {
                Node n = resNodes.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element) n;
                    NodeList resInner = el.getElementsByTagNameNS("http://www.strom.ch", "Resolution");
                    if (resInner.getLength() > 0) {
                        minuten = Integer.parseInt(resInner.item(0).getTextContent());
                        break;
                    }
                }
            }

            NodeList obsNodes = doc.getElementsByTagNameNS("http://www.strom.ch", "Observation");
            for (int i = 0; i < obsNodes.getLength(); i++) {
                Element obs = (Element) obsNodes.item(i);
                String volStr = obs.getElementsByTagNameNS("http://www.strom.ch", "Volume").item(0).getTextContent();
                double relativ = Double.parseDouble(volStr);
                LocalDateTime ts = startTime.plusMinutes((long) i * minuten);
                messwerte.add(new Messwert(ts, relativ, null));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ParsedSDAT(documentId, messwerte);
    }
}
