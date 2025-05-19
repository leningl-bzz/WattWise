package ch.bzz.backend.parser;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ESLParser {

    public static Map<String, Double> parseESLFile(File xmlFile) {
        Map<String, Double> obisValues = new HashMap<>();
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList valueRows = doc.getElementsByTagName("ValueRow");

            for (int i = 0; i < valueRows.getLength(); i++) {
                Node node = valueRows.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String obis = element.getAttribute("obis");
                    String valueStr = element.getAttribute("value");
                    if (!obis.isEmpty() && !valueStr.isEmpty()) {
                        try {
                            double value = Double.parseDouble(valueStr);
                            obisValues.put(obis, value);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid numeric value: " + valueStr);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return obisValues;
    }
}
