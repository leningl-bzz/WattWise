package ch.bzz.backend.parser;

import ch.bzz.backend.model.Messwert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MesswertMerger {

    public static List<Messwert> verknuepfenMitESL(List<Messwert> sdatWerte, Map<String, Double> eslMap, String obis1, String obis2) {
        double startwert = eslMap.getOrDefault(obis1, 0.0) + eslMap.getOrDefault(obis2, 0.0);
        double summe = startwert;
        List<Messwert> ergebnis = new ArrayList<>();

        for (Messwert mw : sdatWerte) {
            summe += mw.getRelativ();
            ergebnis.add(new Messwert(mw.getTimestamp(), mw.getRelativ(), summe));
        }

        return ergebnis;
    }
}
