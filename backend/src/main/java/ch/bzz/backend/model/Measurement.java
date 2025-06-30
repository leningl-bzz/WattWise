package ch.bzz.backend.model;

import java.time.LocalDateTime;

public class Measurement {
    private LocalDateTime timestamp;
    private Double relative; // Renamed from relativeValue for consistency, but field was already 'relative'
    private Double absolute; // Renamed from absoluteValue for consistency, but field was already 'absolute'

    public Measurement(LocalDateTime timestamp, Double relative, Double absolute) {
        this.timestamp = timestamp;
        this.relative = relative;
        this.absolute = absolute;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    // Keep only the primary getters for 'relative' and 'absolute'
    public Double getRelative() {
        return relative;
    }

    public Double getAbsolute() {
        return absolute;
    }

    public void setAbsolute(Double absolute) {
        this.absolute = absolute;
    }

    // Removed redundant getRelativeValue(), getAbsoluteValue(), setAbsoluteValue()
    // Jackson will pick up getRelative() and getAbsolute() for serialization to "relative" and "absolute"

    @Override
    public String toString() {
        return "Measurement{" +
                "timestamp=" + timestamp +
                ", relative=" + relative +
                ", absolute=" + absolute +
                '}';
    }
}