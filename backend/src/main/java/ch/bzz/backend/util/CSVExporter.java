package ch.bzz.backend.util;

import ch.bzz.backend.model.Measurement;
import java.util.List;

public class CSVExporter {
    public static String toCSV(List<Measurement> measurements) {
        StringBuilder sb = new StringBuilder();
        sb.append("Timestamp,Relative,Absolute\n");
        for (Measurement m : measurements) {
            sb.append(m.getTimestamp()).append(',');
            sb.append(m.getRelative()).append(',');
            Double abs = m.getAbsolute();
            sb.append(abs != null ? abs : "");
            sb.append('\n');
        }
        return sb.toString();
    }
}
