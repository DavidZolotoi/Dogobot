package ru.dogobot.Dogobot.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.context.annotation.Configuration;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Setter
@Getter
@Configuration
public class JsonData {
    final String filePath = "../.hidden/.hidden5.json";

    private Map<String, String> settings;

        /**
     * Проверить существование словаря настроек и ключей в нём.
     * Если словаря нет, то создать.
     * Если ключей в словаре нет, то добавить, прочитав их значения из файла.
     * @param jsonKeys ключи, которые необходимо проверить или добавить
     * @return Проверенный и дополненный в случае необходимости словарь.
     */
    public Map<String, String> updateSettings(String... jsonKeys) {
        if (settings == null) settings = new HashMap<>();
        for (var jsonKey:jsonKeys) {
            if (!settings.containsKey(jsonKey))
                updateJsonRow(settings, this.filePath, jsonKey);
        }
        return settings;
    }

    /**
     * Добавить ключ-значение в словарь (1 строка из JSON-файла)
     * Вынесено в отдельный метод, потому что есть повторный вызов, происходит обработка ошибок и логирование
     * @param settingsMap словарь
     * @param filePath путь к файлу JSON
     * @param jsonKey ключ
     */
    private void updateJsonRow(Map<String, String> settingsMap, String filePath, String jsonKey) {
        try{
            settingsMap.put(jsonKey, readJSONFile(filePath, jsonKey));
        }
        catch (Exception e) {
            log.error("Не получается добавить ключ: " + jsonKey + " в словарь." + System.lineSeparator() + e.getMessage());
        }
    }

    /**
     * Метод, получающий значение по ключу из JSON-файла
     * @param filePath путь к файлу JSON,
     * @param jsonKey ключ, по которому необходимо найти значение
     * @return искомое значение
     */
    protected String readJSONFile(String filePath, String jsonKey) {
        String jsonValue = null;
        try (FileReader reader = new FileReader(filePath))
        {
            JSONTokener tokener = new JSONTokener(reader);
            JSONObject jsonObject = new JSONObject(tokener);
            jsonValue = jsonObject.getString(jsonKey);
        } catch (FileNotFoundException e) {
            log.error("Файл не найден: " + filePath + System.lineSeparator() + e.getMessage());
        } catch (JSONException e) {
            log.error("Ошибка при распознавании JSON. Ключ: " + jsonKey + System.lineSeparator() + e.getMessage());
        } catch (IOException e) {
            log.error("Проблема с вводом-выводом при чтении файла: " + filePath + System.lineSeparator() + e.getMessage());
        }
        return jsonValue;
    }

}
