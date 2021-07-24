package com.example.suburbanbot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@lombok.Value
class SuburbanTrainThread {
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

    private final WebClient webClient;

    private SuburbanTrainThread forwardThread() {
        return new SuburbanTrainThread(firstStationCode, secondStationCode);
    }

    private SuburbanTrainThread backwardThread() {
        return new SuburbanTrainThread(secondStationCode, firstStationCode);
    }

    public YandexRaspClient(@Autowired WebClient webClient) {
        this.webClient = webClient;
    }

    public Iterable<String> getNearestThreeTrainsArrivalTime(ZonedDateTime fromTime)
            throws JsonProcessingException {
        return getTrainsArrivalTime(fromTime, forwardThread());
    }

    public Iterable<String> getNearestThreeTrainsArrivalTimeBackward(ZonedDateTime fromTime)
            throws JsonProcessingException{
        return getTrainsArrivalTime(fromTime, backwardThread());
    }

    @NotNull
    private Iterable<String> getTrainsArrivalTime(ZonedDateTime fromTime, SuburbanTrainThread trainThread)
            throws JsonProcessingException {
        String jsonString = requestYandexRasp(trainThread);
        return parseJsonForArrivalTime(fromTime, jsonString);
    }

    @Nullable
    private String requestYandexRasp(SuburbanTrainThread trainThread) {
        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("from", trainThread.getFromStation())
                        .queryParam("to", trainThread.getToStation())
                        .queryParam("apikey", apiKey)
                        .queryParam("transport_types", "suburban")
                        .queryParam("date", new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
                        .build()
                )
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(Throwable::printStackTrace)
                .block();

    }

    @NotNull
    private Iterable<String> parseJsonForArrivalTime(ZonedDateTime fromTime, String jsonString)
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