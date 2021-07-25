package com.example.suburbanbot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
public class YandexRaspClient {

    @Value("${yandex-rasp.key}")
    private String apiKey;

    @Value("${target-TZ}")
    private String userTzCode;

    private final WebClient webClient;
    private final StationsConfig stationsConfig;

    public YandexRaspClient(WebClient webClient, StationsConfig stationsConfig) {
        this.webClient = webClient;
        this.stationsConfig = stationsConfig;
    }

    public DeparturesMessage getNearestThreeTrainsDepartureTime(ZonedDateTime fromTime)
            throws JsonProcessingException {
        return getTrainsDeparturesMessage(fromTime, stationsConfig.forward());
    }

    public DeparturesMessage getNearestThreeTrainsDepartureTimeBackward(ZonedDateTime fromTime)
            throws JsonProcessingException{
        return getTrainsDeparturesMessage(fromTime, stationsConfig.backward());
    }

    @NotNull
    private DeparturesMessage getTrainsDeparturesMessage(ZonedDateTime fromTime, TrainThread trainThread)
            throws JsonProcessingException {
        String jsonString = requestYandexRasp(trainThread);
        Stream<DepartureInfo> departureInfos = parseJsonForDepartureInfo(fromTime, jsonString);
        return new DeparturesMessage(trainThread, departureInfos);
    }

    @Nullable
    private String requestYandexRasp(TrainThread trainThread) {
        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("from", trainThread.getFromStation().getCode())
                        .queryParam("to", trainThread.getToStation().getCode())
                        .queryParam("apikey", apiKey)
                        .queryParam("transport_types", "suburban")
                        .queryParam("date", ZonedDateTime.now(ZoneId.of(userTzCode)).toLocalDate())
                        .build()
                )
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(Throwable::printStackTrace)
                .block();

    }

    @NotNull
    private Stream<DepartureInfo> parseJsonForDepartureInfo(ZonedDateTime fromTime, String jsonString)
            throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonString);
        return StreamSupport
                .stream(root.get("segments").spliterator(), false)
                .map(node -> {
                    String departureDateTimeAsText = node.get("departure").asText();
                    ZonedDateTime departureZonedDateTime = ZonedDateTime.parse(departureDateTimeAsText);
                    boolean notExpress = node.get("thread").get("express_type").isNull();
                    return new DepartureInfo(departureZonedDateTime, !notExpress);
                })
                .dropWhile(depInfo -> depInfo.getDepartureTime().isBefore(fromTime))
                .limit(3);
    }
}