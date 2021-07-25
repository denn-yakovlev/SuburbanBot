package com.example.suburbanbot;


import com.fasterxml.jackson.core.JsonProcessingException;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
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
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        Stream<DepartureInfo> actual = yandexRaspClient.getDepartureInfos(
                ZonedDateTime.parse("2017-03-28T05:00+03:00"),
                new TrainThread(new Station(), new Station()),
                3
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
        Stream<DepartureInfo> actual = yandexRaspClient.getDepartureInfos(
                ZonedDateTime.parse("2017-03-28T05:00+03:00"),
                new TrainThread(new Station(), new Station()),
                3
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
        Stream<DepartureInfo> actual = yandexRaspClient.getDepartureInfos(
                ZonedDateTime.parse("2017-03-28T05:00+03:00"),
                new TrainThread(new Station(), new Station()),
                3
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
        Stream<DepartureInfo> actual = yandexRaspClient.getDepartureInfos(
                ZonedDateTime.parse("2017-03-28T05:00+03:00"),
                new TrainThread(new Station(), new Station()),
                3
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
                () -> yandexRaspClient.getDepartureInfos(
                        ZonedDateTime.parse("2017-03-28T05:00+03:00"),
                        new TrainThread(new Station(), new Station()),
                        3
                )
        );
    }

    @Test
    void testSuburbanTrainsService_shouldReturnNearestThreeTrains() throws JsonProcessingException{
        Station station1 = new Station();
        station1.setName("st1");
        station1.setCode("123");

        Station station2 = new Station();
        station2.setName("st2");
        station2.setCode("456");

        StationsConfig stationsConfig = new StationsConfig();
        stationsConfig.setFirst(station1);
        stationsConfig.setSecond(station2);

        ZonedDateTime currentDateTime = ZonedDateTime.parse("2021-01-01T06:00+03:00");
        Stream<DepartureInfo> depInfos = Stream.of(
                new DepartureInfo(ZonedDateTime.parse("2021-01-01T06:05+03:00"), false),
                new DepartureInfo(ZonedDateTime.parse("2021-01-01T06:07+03:00"), true),
                new DepartureInfo(ZonedDateTime.parse("2021-01-01T06:08+03:00"), true)
        );
        YandexRaspClient mockYandexRaspClient = Mockito.mock(YandexRaspClient.class);
        when(
                mockYandexRaspClient.getDepartureInfos(eq(currentDateTime), any(), anyInt())
        ).thenReturn(depInfos);

        SuburbanTrainsService sut = new SuburbanTrainsService(stationsConfig, mockYandexRaspClient);

        DeparturesMessage actual = sut.getNearestThreeTrainsDepartureTime(currentDateTime);
        DeparturesMessage expected = new DeparturesMessage(stationsConfig.forward(), depInfos);

        assertEquals(expected, actual);
        assertEquals("st1", actual.getTrainThread().getFromStation().getName());
        assertEquals("st2", actual.getTrainThread().getToStation().getName());
    }

    @Test
    void testSuburbanTrainsService_shouldReturnNearestThreeTrainsBackward() throws JsonProcessingException{
        Station station1 = new Station();
        station1.setName("st1");
        station1.setCode("123");

        Station station2 = new Station();
        station2.setName("st2");
        station2.setCode("456");

        StationsConfig stationsConfig = new StationsConfig();
        stationsConfig.setFirst(station1);
        stationsConfig.setSecond(station2);

        ZonedDateTime currentDateTime = ZonedDateTime.parse("2021-01-01T06:00+03:00");
        Stream<DepartureInfo> depInfos = Stream.of(
                new DepartureInfo(ZonedDateTime.parse("2021-01-01T06:05+03:00"), false),
                new DepartureInfo(ZonedDateTime.parse("2021-01-01T06:07+03:00"), true),
                new DepartureInfo(ZonedDateTime.parse("2021-01-01T06:08+03:00"), true)
        );
        YandexRaspClient mockYandexRaspClient = mock(YandexRaspClient.class);
        when(
                mockYandexRaspClient.getDepartureInfos(eq(currentDateTime), eq(stationsConfig.backward()), anyInt())
        ).thenReturn(depInfos);

        SuburbanTrainsService sut = new SuburbanTrainsService(stationsConfig, mockYandexRaspClient);

        DeparturesMessage actual = sut.getNearestThreeTrainsDepartureTimeBackward(currentDateTime);
        DeparturesMessage expected = new DeparturesMessage(stationsConfig.backward(), depInfos);

        assertEquals(expected, actual);
        assertEquals("st2", actual.getTrainThread().getFromStation().getName());
        assertEquals("st1", actual.getTrainThread().getToStation().getName());
    }
}
