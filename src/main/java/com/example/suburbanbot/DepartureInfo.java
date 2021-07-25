package com.example.suburbanbot;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@lombok.Value
public class DepartureInfo {
    ZonedDateTime departureTime;
    boolean isExpressTrain;

    @Override
    public String toString() {
        String timeAsString = departureTime.toLocalTime().format(
                DateTimeFormatter.ofPattern("HH:mm")
        );
        return timeAsString + (isExpressTrain ? " (экспресс)" : "");
    }
}
