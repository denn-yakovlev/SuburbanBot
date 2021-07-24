package com.example.suburbanbot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
public class YandexRaspClient {

    @Value("${stations.from.code}")
    private String fromStationCode;
    @Value("${stations.to.code}")
    private String toStationCode;

    @Value("${yandex-rasp.key}")
    private String apiKey;

    private WebClient webClient;

    public YandexRaspClient(@Autowired WebClient webClient) {
        this.webClient = webClient;
    }

    public Iterable<String> getNearestThreeSuburbanTrainsArrivalTime(LocalTime fromTime)
            throws JsonProcessingException {
        String jsonString = webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("from", fromStationCode)
                        .queryParam("to", toStationCode)
                        .queryParam("apikey", apiKey)
                        .queryParam("transport_types", "suburban")
                        .queryParam("date", new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
                        .build()
                )
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(Throwable::printStackTrace)
                .block();
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