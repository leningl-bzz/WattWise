package ch.bzz.backend.model;

import java.time.LocalDateTime;

public class Messwert {
    private LocalDateTime timestamp;
    private Double relativ;
    private Double absolut;

    public Messwert(LocalDateTime timestamp, Double relativ, Double absolut) {
        this.timestamp = timestamp;
        this.relativ = relativ;
        this.absolut = absolut;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Double getRelativ() {
        return relativ;
    }

    public Double getAbsolut() {
        return absolut;
    }

    public void setAbsolut(Double absolut) {
        this.absolut = absolut;
    }

    @Override
    public String toString() {
        return "Messwert{" +
                "timestamp=" + timestamp +
                ", relativ=" + relativ +
                ", absolut=" + absolut +
                '}';
    }
}
