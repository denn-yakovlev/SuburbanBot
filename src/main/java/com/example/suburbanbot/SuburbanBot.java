package com.example.suburbanbot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.telegram.abilitybots.api.objects.Locality.*;
import static org.telegram.abilitybots.api.objects.Privacy.*;

@Component
public class SuburbanBot extends AbilityBot {

    private YandexRaspClient yandexRaspClient;

    protected SuburbanBot(
            @Value("${bot.token}") String botToken,
            @Value("${bot.name}") String botUsername,
            @Autowired YandexRaspClient yandexApiClient
    ) {
        super(botToken, botUsername);
        this.yandexRaspClient = yandexApiClient;
    }

    public Ability sayHelloWorld() {
        return Ability
                .builder()
                .name("hello")
                .info("says hello world!")
                .input(0)
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> silent.send("Hello world!", ctx.chatId()))
                .post(ctx -> silent.send("Bye!", ctx.chatId()))
                .build();
    }

    public Ability getThreeNearestTrainsArrivalTime() {
        return Ability
                .builder()
                .name("fwd")
                .info("Время прибытия к станции отправления трёх ближайших электричек")
                .input(0)
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> {
                            String message = "";
                            try {
                                message = String.join(
                                        System.lineSeparator(),
                                        yandexRaspClient.getNearestThreeSuburbanTrainsArrivalTime(LocalTime.now())
                                );
                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                                message = "Ошибка обработки ответа от внешнего API";
                            } finally {
                                silent.send(message, ctx.chatId());
                            }
                        }
                )
                .build();
    }

    @Override
    public long creatorId() {
        return 0;
    }
}