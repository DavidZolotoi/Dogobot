package ru.dogobot.Dogobot.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@Data
public class EmailConfig {

    private String smtpHost;
    private int smtpPort;
    private String imapHost;
    private int imapPort;
    private String email;
    private String password;

    @Autowired
    public EmailConfig(JsonData jsonData) {
        jsonData.setSettings(jsonData.updateSettings("DOGOBOT4", "DOGOBOT5", "DOGOBOT6", "DOGOBOT7", "DOGOBOT8", "DOGOBOT9"));
        try {
            this.smtpHost = jsonData.getSettings().get("DOGOBOT4");
            this.smtpPort = Integer.parseInt(jsonData.getSettings().get("DOGOBOT5"));
            this.imapHost = jsonData.getSettings().get("DOGOBOT6");
            this.imapPort = Integer.parseInt(jsonData.getSettings().get("DOGOBOT7"));
            this.email = jsonData.getSettings().get("DOGOBOT8");
            this.password = jsonData.getSettings().get("DOGOBOT9");
        }
        catch (Exception e) {
            log.error("Не получается получить данные почты из jsonData.getSettings() или не получается Integer.parseInt(..)." + System.lineSeparator() + e.getMessage());
        }
    }
}
