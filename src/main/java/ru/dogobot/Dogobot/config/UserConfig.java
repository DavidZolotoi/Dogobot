package ru.dogobot.Dogobot.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@Data
public class UserConfig {

    //todo перепроверить методы изменения и убедиться, что изменения идут и в базу и в json
    // потому что при загрузке данные берутся из json и обновляются в базе

    private String packPassword;
    private String personalEmail;
    private String otherEmail;

    @Autowired
    public UserConfig(JsonData jsonData) {
        jsonData.setSettings(jsonData.updateSettings("DOGOBOT11", "DOGOBOT12", "DOGOBOT13"));
        try {
            this.packPassword = jsonData.getSettings().get("DOGOBOT11");
            this.personalEmail = jsonData.getSettings().get("DOGOBOT12");
            this.otherEmail = jsonData.getSettings().get("DOGOBOT13");
        }
        catch (Exception e) {
            log.error("Не получается получить данные по умолчанию для пользователя." + System.lineSeparator() + e.getMessage());
        }

    }
}
