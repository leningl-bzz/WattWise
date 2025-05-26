package ch.bzz.backend.model;

import java.time.LocalDateTime;

public class Measurement {
    private LocalDateTime timestamp;
    private Double relative;
    private Double absolute;

    public Measurement(LocalDateTime timestamp, Double relative, Double absolute) {
        this.timestamp = timestamp;
        this.relative = relative;
        this.absolute = absolute;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Double getRelative() {
        return relative;
    }

    public Double getAbsolute() {
        return absolute;
    }

    public void setAbsolute(Double absolute) {
        this.absolute = absolute;
    }

    public Double getRelativeValue() {
        return getRelative();
    }

    public Double getAbsoluteValue() {
        return getAbsolute();
    }

    public void setAbsoluteValue(double absolute) {
        setAbsolute(absolute);
    }

    @Override
    public String toString() {
        return "Measurement{" +
                "timestamp=" + timestamp +
                ", relative=" + relative +
                ", absolute=" + absolute +
                '}';
    }
}
