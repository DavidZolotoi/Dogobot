package ru.dogobot.Dogobot.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@Data
public class UserDefaultConfig {
    
    private String packPassword;

    @Autowired
    public UserDefaultConfig(JsonData jsonData) {
        jsonData.setSettings(jsonData.updateSettings("DOGOBOT11"));
        try {
            this.packPassword = jsonData.getSettings().get("DOGOBOT11");
        }
        catch (Exception e) {
            log.error("Не получается получить данные по умолчанию для пользователя." + System.lineSeparator() + e.getMessage());
        }
    } 

}
