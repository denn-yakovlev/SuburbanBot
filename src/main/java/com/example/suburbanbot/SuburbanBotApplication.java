package com.example.suburbanbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@EnableConfigurationProperties(StationsConfig.class)
public class SuburbanBotApplication {

    @Bean
    public WebClient getWebClient() {
        return WebClient.create("https://api.rasp.yandex.net/v3.0/search/");
    }
    public static void main(String[] args) {
        SpringApplication.run(SuburbanBotApplication.class, args);
    }

}
