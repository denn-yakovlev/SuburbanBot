package com.example.suburbanbot;


import com.fasterxml.jackson.core.JsonProcessingException;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@RunWith(SpringRunner.class)
class SuburbanBotApplicationTests {
    private static MockWebServer mockServer;

    private static HttpUrl baseUrl;

    @Configuration
    @Import(SuburbanBotApplication.class)
    public static class TestConfig {
        @Bean
        @Primary
        public WebClient mockWebClient() {
            return WebClient.create(baseUrl.toString());
        }
    }

    @Autowired
    public YandexRaspClient yandexRaspClient;

    @BeforeAll
    static void setUp() throws IOException {
        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
        mockServer = new MockWebServer();
        mockServer.start();
        baseUrl = mockServer.url("/");
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockServer.shutdown();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void testYandexApiClient_shouldReturnOneResult_whenOneArrivalGiven() throws IOException {
        String json = Files.readString(
                Paths.get(
                        System.getProperty("user.dir"), "src/test/resources/", "suburban_bot_test.json"
                )
        );
        mockServer.enqueue(new MockResponse()
                .setBody(json)
                .setHeader("Content-Type", "application/json")
        );
        Stream<DepartureInfo> expected = Stream.of(
                new DepartureInfo(ZonedDateTime.parse("2017-03-28T06:00+03:00"), false)
        );
        Stream<DepartureInfo> actual = yandexRaspClient.getNearestThreeTrainsDepartureTime(
                ZonedDateTime.parse("2017-03-28T05:00+03:00")
        );
        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    void testYandexApiClient_shouldReturnThreeResults_whenThreeArrivalGiven() throws IOException {
        String json = Files.readString(
                Paths.get(
                        System.getProperty("user.dir"), "src/test/resources/", "suburban_bot_test2.json"
                )
        );
        mockServer.enqueue(new MockResponse()
                .setBody(json)
                .setHeader("Content-Type", "application/json")
        );
        Stream<DepartureInfo> expected = Stream.of(
                new DepartureInfo(ZonedDateTime.parse("2017-03-28T06:00+03:00"), false),
                new DepartureInfo(ZonedDateTime.parse("2017-03-28T06:01+03:00"), false),
                new DepartureInfo(ZonedDateTime.parse("2017-03-28T06:02+03:00"), false)
                );
        Stream<DepartureInfo> actual = yandexRaspClient.getNearestThreeTrainsDepartureTime(
                ZonedDateTime.parse("2017-03-28T05:00+03:00")
        );
        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    void testYandexApiClient_shouldReturnThreeResults_whenFourArrivalGiven() throws IOException {
        String json = Files.readString(
                Paths.get(
                        System.getProperty("user.dir"), "src/test/resources/", "suburban_bot_test3.json"
                )
        );
        mockServer.enqueue(new MockResponse()
                .setBody(json)
                .setHeader("Content-Type", "application/json")
        );
        Stream<DepartureInfo> expected = Stream.of(
                new DepartureInfo(ZonedDateTime.parse("2017-03-28T06:00+03:00"), false),
                new DepartureInfo(ZonedDateTime.parse("2017-03-28T06:01+03:00"), false),
                new DepartureInfo(ZonedDateTime.parse("2017-03-28T06:02+03:00"), false)
                );
        Stream<DepartureInfo> actual = yandexRaspClient.getNearestThreeTrainsDepartureTime(
                ZonedDateTime.parse("2017-03-28T05:00+03:00")
        );
        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    void testYandexApiClient_shouldReturnNoResults_whenZeroArrivalGiven() throws IOException {
        String json = Files.readString(
                Paths.get(
                        System.getProperty("user.dir"), "src/test/resources/", "suburban_bot_test4.json"
                )
        );
        mockServer.enqueue(new MockResponse()
                .setBody(json)
                .setHeader("Content-Type", "application/json")
        );
        Stream<DepartureInfo> expected = Stream.empty();
        Stream<DepartureInfo> actual = yandexRaspClient.getNearestThreeTrainsDepartureTime(
                ZonedDateTime.parse("2017-03-28T05:00+03:00")
        );
        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    void testYandexApiClient_shouldThrow_whenInvalidJson() throws IOException {
        String json = Files.readString(
                Paths.get(
                        System.getProperty("user.dir"), "src/test/resources/", "suburban_bot_test5.json"
                )
        );
        mockServer.enqueue(new MockResponse()
                .setBody(json)
                .setHeader("Content-Type", "application/json")
        );
        assertThrows(
                JsonProcessingException.class,
                () -> yandexRaspClient.getNearestThreeTrainsDepartureTime(
                        ZonedDateTime.parse("2017-03-28T05:00+03:00")
                )
        );
    }
}
