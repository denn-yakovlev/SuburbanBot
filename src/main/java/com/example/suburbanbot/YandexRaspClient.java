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
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    public Iterable<String> getNearestThreeTrainsArrivalTime(LocalTime fromTime)
            throws JsonProcessingException {
        return getTrainsArrivalTime(fromTime, forwardThread());
    }

    public Iterable<String> getNearestThreeTrainsArrivalTimeBackward(LocalTime fromTime)
            throws JsonProcessingException{
        return getTrainsArrivalTime(fromTime, backwardThread());
    }

    @NotNull
    private Iterable<String> getTrainsArrivalTime(LocalTime fromTime, SuburbanTrainThread trainThread)
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
    private Iterable<String> parseJsonForArrivalTime(LocalTime fromTime, String jsonString)
            throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonString);
        return StreamSupport
                .stream(root.get("segments").spliterator(), false)
                .map(node -> node.get("departure").asText())
                .map(arrivalDateTimeAsStr -> LocalDateTime.parse(
                        arrivalDateTimeAsStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME
                ))
                .map(LocalDateTime::toLocalTime)
                .dropWhile(localTime-> localTime.isBefore(fromTime))
                .limit(3)
                .map(localDt -> localDt.format(DateTimeFormatter.ofPattern("HH:mm")))
                .collect(Collectors.toList());
    }
}