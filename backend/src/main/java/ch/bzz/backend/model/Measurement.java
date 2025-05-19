package ch.bzz.backend.model;

import java.time.LocalDateTime;

/* Einzelner Messwert */
public class Measurement {
    private LocalDateTime timestamp;
    private double relativeValue;
    private Double absoluteValue;

    public Measurement(LocalDateTime timestamp, double relativeValue, Double absoluteValue) {
        this.timestamp = timestamp;
        this.relativeValue = relativeValue;
        this.absoluteValue = absoluteValue;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public double getRelativeValue() {
        return relativeValue;
    }

    public void setRelativeValue(double relativeValue) {
        this.relativeValue = relativeValue;
    }

    public Double getAbsoluteValue() {
        return absoluteValue;
    }

    public void setAbsoluteValue(Double absoluteValue) {
        this.absoluteValue = absoluteValue;
    }

    public void setAbsoluteValue(double value) {
        this.absoluteValue = value;
    }
}
