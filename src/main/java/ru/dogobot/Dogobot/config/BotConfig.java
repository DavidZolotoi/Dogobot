package ru.dogobot.Dogobot.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@Configuration
@EnableScheduling
@Data
public class BotConfig {
//    final String filePath = "../.hidden/.hidden";
    final String filePath = "/home/delllindeb/archive/myproject/Dogobot/.hidden/.hidden";
    String botName;
    String token;
    Long ownerId;

    @Autowired
    public BotConfig(JsonData jsonData) {
        jsonData.setSettings(jsonData.updateSettings(filePath,"DOGOBOT1", "DOGOBOT2", "DOGOBOT3"));
        try {
            this.botName = jsonData.getSettings().get("DOGOBOT1");
            this.token = jsonData.getSettings().get("DOGOBOT2");
            this.ownerId = Long.parseLong(jsonData.getSettings().get("DOGOBOT3"));
        }
        catch (Exception e) {
            log.error("Не получается получить данные из jsonData.getSettings() или не получается Long.parseLong(..)." + System.lineSeparator() + e.getMessage());
        }
    }
}
