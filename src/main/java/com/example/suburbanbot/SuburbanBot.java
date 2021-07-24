package com.example.suburbanbot;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;

import java.time.LocalTime;

import static org.telegram.abilitybots.api.objects.Locality.*;
import static org.telegram.abilitybots.api.objects.Privacy.*;

@FunctionalInterface
interface ArrivalTimeProvider {
    Iterable<String> getArrivalTime(LocalTime fromTime) throws JsonProcessingException;
}

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
        return getArrivalTimeAbility(
                "fwd",
                "Время прибытия к станции отправления трёх ближайших электричек",
                yandexRaspClient::getNearestThreeTrainsArrivalTime
        );
    }

    public Ability getThreeNearestTrainsArrivalTimeBackWards() {
        return getArrivalTimeAbility(
                "bwd",
                "Время прибытия к станции отправления трёх ближайших электричек (обратное направление)",
                yandexRaspClient::getNearestThreeTrainsArrivalTimeBackward
        );
    }

    private Ability getArrivalTimeAbility(
            String commandName,
            String commandDescription,
            ArrivalTimeProvider resultProvider
    ) {
        return Ability
                .builder()
                .name(commandName)
                .info(commandDescription)
                .input(0)
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> {
                            String message = "";
                            try {
                                message = String.join(
                                        System.lineSeparator(),
                                        resultProvider.getArrivalTime(LocalTime.now())
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