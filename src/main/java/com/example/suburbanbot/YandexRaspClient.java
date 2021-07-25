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
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@lombok.Value
class TrainThread {
    String fromStation;
    String toStation;
}

@Component
public class YandexRaspClient {

    @Value("${stations.first.code}")
    private String firstStationCode;
    @Value("${stations.second.code}")
    private String secondStationCode;

    @Value("${yandex-rasp.key}")
    private String apiKey;

    @Value("${target-TZ}")
    private String userTzCode;

    private TrainThread forwardThread() {
        return new TrainThread(firstStationCode, secondStationCode);
    }

    private TrainThread backwardThread() {
        return new TrainThread(secondStationCode, firstStationCode);
    }

    private final WebClient webClient;

    public YandexRaspClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Iterable<String> getNearestThreeTrainsDepartureTime(ZonedDateTime fromTime)
            throws JsonProcessingException {
        return getTrainsDepartureTime(fromTime, forwardThread());
    }

    public Iterable<String> getNearestThreeTrainsDepartureTimeBackward(ZonedDateTime fromTime)
            throws JsonProcessingException{
        return getTrainsDepartureTime(fromTime, backwardThread());
    }

    @NotNull
    private Iterable<String> getTrainsDepartureTime(ZonedDateTime fromTime, TrainThread trainThread)
            throws JsonProcessingException {
        String jsonString = requestYandexRasp(trainThread);
        return parseJsonForDepartureTime(fromTime, jsonString);
    }

    @Nullable
    private String requestYandexRasp(TrainThread trainThread) {
        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("from", trainThread.getFromStation())
                        .queryParam("to", trainThread.getToStation())
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
    private Iterable<String> parseJsonForDepartureTime(ZonedDateTime fromTime, String jsonString)
            throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonString);
        return StreamSupport
                .stream(root.get("segments").spliterator(), false)
                .map(node -> node.get("departure").asText())
                .map(ZonedDateTime::parse)
                .dropWhile(zonedDt -> zonedDt.isBefore(fromTime))
                .limit(3)
                .map(zonedDt -> zonedDt
                        .withZoneSameInstant(ZoneId.of(userTzCode))
                        .toLocalTime()
                        .format(DateTimeFormatter.ofPattern("HH:mm"))
                )
                .collect(Collectors.toList());
    }
}