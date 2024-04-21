package ru.dogobot.Dogobot.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import ru.dogobot.Dogobot.service.Jsoner;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@Data
public class UserConfig {

    private Jsoner jsoner;
    final String FILE_PATH = "../.hidden/.hidden5.json";
    final int INDENT_FACTOR = 2;
    private Map<String, String> configs;

    //Изменяемые параметры пользователя, которые хранятся не только в БД, но в JSON

    private final String PACK_PASSWORD_KEY = "PACK_PASSWORD_KEY";
    private String packPassword;

    private final String PERSONAL_EMAIL_KEY = "PERSONAL_EMAIL_KEY";
    private String personalEmail;

    private final String OTHER_EMAIL_KEY = "OTHER_EMAIL_KEY";
    private String otherEmail;

    @Autowired
    public UserConfig(Jsoner jsoner) {
        this.jsoner = jsoner;
        this.configs = readConfigs(this.configs, PACK_PASSWORD_KEY, PERSONAL_EMAIL_KEY, OTHER_EMAIL_KEY);
        try {
            this.packPassword = this.configs.get(PACK_PASSWORD_KEY);
            this.personalEmail = this.configs.get(PERSONAL_EMAIL_KEY);
            this.otherEmail = this.configs.get(OTHER_EMAIL_KEY);
        }
        catch (Exception e) {
            log.error("Не получается получить данные по умолчанию для пользователя." + System.lineSeparator() + e.getMessage());
        }
    }

    /**
     * Проверяет существование словаря настроек и ключей в нём.
     * Если словаря нет, то создаёт его.
     * Если ключей в словаре нет, то добавляет их, прочитав значения из файла.
     * @param configs словарь настроек
     * @param jsonKeys ключи, которые необходимо проверить или добавить
     * @return Проверенный и дополненный, в случае необходимости, словарь.
     */
    public Map<String, String> readConfigs(Map<String, String> configs, String... jsonKeys) {
        if (configs == null) configs = new HashMap<>();
        for (var jsonKey:jsonKeys) {
            if (!configs.containsKey(jsonKey)) {
                try{
                    configs.put(jsonKey, jsoner.readJSONFile(FILE_PATH, jsonKey));
                }
                catch (Exception e) {
                    log.error("Не получается добавить ключ: " + jsonKey + " в словарь." + System.lineSeparator() + e.getMessage());
                }
            }
        }
        return configs;
    }

    public void updateConfig(String key, String value) {
        this.configs.put(key, value);
        this.jsoner.updateValueJSONFile(this.FILE_PATH, this.INDENT_FACTOR, key, value);
    }

    public void updatePackPassword(String packPassword) {
        this.packPassword = packPassword;
        this.updateConfig(PACK_PASSWORD_KEY, packPassword);
    }

    public void updatePersonalEmail(String personalEmail) {
        this.personalEmail = personalEmail;
        this.updateConfig(PERSONAL_EMAIL_KEY, personalEmail);
    }

    public void updateOtherEmail(String otherEmail) {
        this.otherEmail = otherEmail;
        this.updateConfig(OTHER_EMAIL_KEY, otherEmail);
    }

}
