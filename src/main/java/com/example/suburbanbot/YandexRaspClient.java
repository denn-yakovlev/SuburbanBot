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
import java.time.format.DateTimeFormatter;
import java.util.Date;
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

    public Stream<String> getNearestThreeSuburbanTrainsArrivalTime() throws JsonProcessingException {
        String jsonString = webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("from", fromStationCode)
                        .queryParam("to", toStationCode)
                        .queryParam("apikey", apiKey)
                        .queryParam("transport_types", "suburban")
                        .queryParam("limit", 3)
                        .queryParam("date", new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
                        .build()
                )
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(exc -> exc.printStackTrace())
                .block();
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(jsonString);
                return StreamSupport
                        .stream(root.get("segments").spliterator(), false)
                        .limit(3)
                        .map(node -> node.get("arrival").asText())
                        .map(arrivalDateAsStr -> LocalDateTime.parse(
                                arrivalDateAsStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME
                        ))
                        .map(localDt -> localDt
                                .toLocalTime()
                                .format(DateTimeFormatter.ofPattern("hh:mm"))
                        );


    }
}