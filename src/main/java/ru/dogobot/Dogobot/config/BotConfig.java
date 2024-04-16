package ru.dogobot.Dogobot.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.util.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@Configuration
@EnableScheduling
@Data
public class BotConfig {

    @Value("${telegrambot.token}")
    String token;
    @Value("${telegrambot.name}")
    String botName;
    @Value("${telegrambot.authorId}")
    Long authorId;
    @Value("${telegrambot.ownerId}")
    Long ownerId;

//    @Autowired
//    public BotConfig(JsonData jsonData) {
//        jsonData.setSettings(jsonData.updateSettings("DOGOBOT1", "DOGOBOT2", "DOGOBOT3", "DOGOBOT10"));
//        try {
//            this.botName = jsonData.getSettings().get("DOGOBOT1");
//            this.token = jsonData.getSettings().get("DOGOBOT2");
//            this.authorId = Long.parseLong(jsonData.getSettings().get("DOGOBOT3"));
//            this.ownerId = Long.parseLong(jsonData.getSettings().get("DOGOBOT10"));
//        }
//        catch (Exception e) {
//            log.error("Не получается получить данные бота из jsonData.getSettings() или не получается Long.parseLong(..)." + System.lineSeparator() + e.getMessage());
//        }
//    }
}
