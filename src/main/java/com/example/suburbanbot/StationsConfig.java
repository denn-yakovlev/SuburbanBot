package com.example.suburbanbot;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("stations")
public class StationsConfig {

    @Setter
    private Station first;

    @Setter
    private Station second;

    public TrainThread forward() {
        return new TrainThread(first, second);
    }

    public TrainThread backward() {
        return new TrainThread(second, first);
    }
}
