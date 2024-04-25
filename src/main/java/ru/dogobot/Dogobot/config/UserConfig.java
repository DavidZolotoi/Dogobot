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

    //region Изменяемые параметры пользователя, которые хранятся не только в БД, но в JSON

    private final String PACK_PASSWORD_KEY = "PACK_PASSWORD_KEY";
    private String packPassword;

    private final String PERSONAL_EMAIL_KEY = "PERSONAL_EMAIL_KEY";
    private String personalEmail;

    private final String OTHER_EMAIL_KEY = "OTHER_EMAIL_KEY";
    private String otherEmail;

    //endregion

    @Autowired
    public UserConfig(Jsoner jsoner) {
        this.jsoner = jsoner;
        this.configs = readConfigs(this.configs, PACK_PASSWORD_KEY, PERSONAL_EMAIL_KEY, OTHER_EMAIL_KEY);

        if (!this.configs.containsKey(PACK_PASSWORD_KEY)){
            log.warn("В словаре отсутствует пароль для упаковки/распаковки.");
        }
        this.packPassword = this.configs.get(PACK_PASSWORD_KEY);

        if (!this.configs.containsKey(PERSONAL_EMAIL_KEY)){
            log.warn("В словаре отсутствует персональная почта.");
        }
        this.personalEmail = this.configs.get(PERSONAL_EMAIL_KEY);

        if (!this.configs.containsKey(OTHER_EMAIL_KEY)){
            log.warn("В словаре отсутствует другая почта.");
        }
        this.otherEmail = this.configs.get(OTHER_EMAIL_KEY);

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
            if (jsonKey == null) continue;
            if (!configs.containsKey(jsonKey)) {
                configs.put(jsonKey, jsoner.readJSONFile(FILE_PATH, jsonKey));
            }
        }
        return configs;
    }

    /**
     * Обновляет пару ключ-значение в словаре настроек и в JSON-файле
     * @param key ключ для обновления
     * @param value значение для обновления
     * @return результат изменения
     */
    public boolean updateMapAndJSON(String key, String value) {
        if (key == null) return false;
        boolean result = false;
        try {
            this.configs.put(key, value);
            result = this.jsoner.updateValueJSONFile(this.FILE_PATH, this.INDENT_FACTOR, key, value);
            log.info("Пара ключ-значение по ключу '%s' обновлена в словаре настроек и в JSON-файле.".formatted(key));
        } catch (Exception e) {
            log.warn("Не удалось обновить/добавить пару ключ-значение по ключу '%s' в словарь или в JSON-файл.%s%s"
                    .formatted(key, System.lineSeparator(), e.getMessage()));
        }
        return result;
    }

    /**
     * Обновляет пару ключ-значение для пароля упаковки/распаковки в конфигурации (поле, словарь настроек и JSON-файле)
     * @param packPassword новое значение пароля
     * @return результат изменения
     */
    public boolean updateConfigPackPassword(String packPassword) {
        this.packPassword = packPassword;
        log.info("Поле packPassword экземпляра UserConfig обновлено.");
        boolean isUpdateMapAndJSON = this.updateMapAndJSON(PACK_PASSWORD_KEY, packPassword);
        if (!isUpdateMapAndJSON){
            log.warn("Не удалось обновить пароль пользователя в конфигурациях: словаре и/или JSON-файле.");
        }
        return isUpdateMapAndJSON;
    }

    /**
     * Обновляет пару ключ-значение для персональной почты в конфигурации (поле, словарь настроек и JSON-файле)
     * @param personalEmail новое значение персональной почты
     * @return результат изменения
     */
    public boolean updateConfigPersonalEmail(String personalEmail) {
        this.personalEmail = personalEmail;
        log.info("Поле personalEmail экземпляра UserConfig обновлено.");
        boolean isUpdateMapAndJSON =  this.updateMapAndJSON(PERSONAL_EMAIL_KEY, personalEmail);
        if (!isUpdateMapAndJSON){
            log.warn("Не удалось обновить персональную почту пользователя в конфигурациях: словаре и/или JSON-файле.");
        }
        return isUpdateMapAndJSON;
    }

    /**
     * Обновляет пару ключ-значение для другой почты в конфигурации (поле, словарь настроек и JSON-файле)
     * @param otherEmail новое значение другой почты
     * @return обновленное значение другой почты
     */
    public boolean updateConfigOtherEmail(String otherEmail) {
        this.otherEmail = otherEmail;
        log.info("Поле otherEmail экземпляра UserConfig обновлено.");
        boolean isUpdateMapAndJSON =  this.updateMapAndJSON(OTHER_EMAIL_KEY, otherEmail);
        if (!isUpdateMapAndJSON){
            log.warn("Не удалось обновить другую почту пользователя в конфигурациях: словаре и/или JSON-файле.");
        }
        return isUpdateMapAndJSON;
    }

    @Override
    public String toString() {
        return "UserConfig{packPassword='%s', personalEmail='%s', otherEmail='%s'}"
                .formatted(packPassword, personalEmail, otherEmail);
    }
}
