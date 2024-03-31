package ru.dogobot.Dogobot.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@Data
public class BotConfig {

    String botName;
    String token;
    Long ownerId;

    @Autowired
    public BotConfig(JsonData jsonData) {
        jsonData.setSettings(jsonData.updateSettings("DOGOBOT1", "DOGOBOT2", "DOGOBOT3"));
        this.botName = jsonData.getSettings().get("DOGOBOT1");
        this.token = jsonData.getSettings().get("DOGOBOT2");
        this.ownerId = Long.parseLong(jsonData.getSettings().get("DOGOBOT3"));
    }
}
