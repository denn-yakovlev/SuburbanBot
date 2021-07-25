package com.example.suburbanbot;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

@Component
class SuburbanTrainsService {
    private final StationsConfig stationsConfig;
    private final YandexRaspClient yandexRaspClient;

    SuburbanTrainsService(StationsConfig stationsConfig, YandexRaspClient yandexRaspClient) {
        this.stationsConfig = stationsConfig;
        this.yandexRaspClient = yandexRaspClient;
    }

    public DeparturesMessage getNearestThreeTrainsDepartureTime(ZonedDateTime fromTime)
            throws JsonProcessingException {
        return getTrainsDeparturesMessage(fromTime, 3, stationsConfig.forward());
    }

    public DeparturesMessage getNearestThreeTrainsDepartureTimeBackward(ZonedDateTime fromTime)
            throws JsonProcessingException {
        return getTrainsDeparturesMessage(fromTime, 3, stationsConfig.backward());
    }

    @NotNull
    private DeparturesMessage getTrainsDeparturesMessage(ZonedDateTime fromTime, int resultsLimit, TrainThread trainThread)
            throws JsonProcessingException {
        Stream<DepartureInfo> departureInfos = yandexRaspClient.getDepartureInfos(fromTime, trainThread, resultsLimit);
        return new DeparturesMessage(trainThread, departureInfos);
    }
}
