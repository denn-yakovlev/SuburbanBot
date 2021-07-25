package com.example.suburbanbot;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;

import java.time.ZonedDateTime;

import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@FunctionalInterface
interface DepartureTimeProvider {
    Iterable<String> getDepartureTime(ZonedDateTime fromTime) throws JsonProcessingException;
}

@Component
public class SuburbanBot extends AbilityBot {

    private YandexRaspClient yandexRaspClient;

    protected SuburbanBot(
            @Value("${bot.token}") String botToken,
            @Value("${bot.name}") String botUsername,
            YandexRaspClient yandexApiClient
    ) {
        super(botToken, botUsername);
        this.yandexRaspClient = yandexApiClient;
    }

    public Ability getThreeNearestTrainsDepartureTime() {
        return getDepartureTimeAbility(
                "fwd",
                "Время отбытия от станции отправления трёх ближайших электричек",
                yandexRaspClient::getNearestThreeTrainsDepartureTime
        );
    }

    public Ability getThreeNearestTrainsDepartureTimeBackWards() {
        return getDepartureTimeAbility(
                "bwd",
                "Время отбытия от станции отправления трёх ближайших электричек (обратное направление)",
                yandexRaspClient::getNearestThreeTrainsDepartureTimeBackward
        );
    }

    private Ability getDepartureTimeAbility(
            String commandName,
            String commandDescription,
            DepartureTimeProvider resultProvider
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
                                        resultProvider.getDepartureTime(ZonedDateTime.now())
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