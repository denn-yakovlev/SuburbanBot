package com.example.suburbanbot;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;

import java.time.ZonedDateTime;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@FunctionalInterface
interface DeparturesMessageProvider {
    DeparturesMessage getDeparturesMessage(ZonedDateTime fromTime) throws JsonProcessingException;
}

@lombok.Value
class DeparturesMessage {
    TrainThread trainThread;
    Stream<DepartureInfo> departures;

    @Override
    public String toString() {
        return String.join(
                System.lineSeparator(),
                trainThread.toString(),
                departures
                        .map(DepartureInfo::toString)
                        .collect(Collectors.joining(System.lineSeparator()))
                );
    }
}

@Component
public class SuburbanBot extends AbilityBot {

    private SuburbanTrainsService suburbanTrainsService;

    protected SuburbanBot(
            @Value("${bot.token}") String botToken,
            @Value("${bot.name}") String botUsername,
            SuburbanTrainsService suburbanTrainsService
    ) {
        super(botToken, botUsername);
        this.suburbanTrainsService = suburbanTrainsService;
    }

    public Ability getThreeNearestTrainsDepartureTime() {
        return getDepartureTimeAbility(
                "fwd",
                "Время отбытия от станции отправления трёх ближайших электричек",
                suburbanTrainsService::getNearestThreeTrainsDepartureTime
        );
    }

    public Ability getThreeNearestTrainsDepartureTimeBackWards() {
        return getDepartureTimeAbility(
                "bwd",
                "Время отбытия от станции отправления трёх ближайших электричек (обратное направление)",
                suburbanTrainsService::getNearestThreeTrainsDepartureTimeBackward
        );
    }

    private Ability getDepartureTimeAbility(
            String commandName,
            String commandDescription,
            DeparturesMessageProvider resultProvider
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
                                message = resultProvider
                                        .getDeparturesMessage(ZonedDateTime.now())
                                        .toString();
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