package ru.dogobot.Dogobot.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JsonData {
    @Getter @Setter
    private Map<String, String> settings;

    /**
     * Проверить существование статического словаря настроек и ключей в нём.
     * Если словаря нет, то создать.
     * Если ключей в словаре нет, то добавить, прочитав их значения из файла.
     * @param mapForUpdate словарь, который необходимо проверить и вернуть.
     * @param filePath путь к файлу, в котором хранятся ключи и значения для словаря
     * @param jsonKeys ключи, которые необходимо проверить или добавить
     * @return Проверенный и дополненный в случае необходимости словарь.
     */
    protected Map<String, String> updateSettings(Map<String, String> mapForUpdate, String filePath, String... jsonKeys) {
        if (mapForUpdate == null) mapForUpdate = new HashMap<>();
        for (var jsonKey:jsonKeys) {
            if (!mapForUpdate.containsKey(jsonKey))
                updateJsonRow(mapForUpdate, filePath, jsonKey);
        }
        return mapForUpdate;
    }

    /**
     * Проверить существование статического словаря настроек и ключей в нём.
     * Если словаря нет, то создать.
     * Если ключей в словаре нет, то добавить, прочитав их значения из файла.
     * @param filePath путь к файлу, в котором хранятся ключи и значения для словаря
     * @param jsonKeys ключи, которые необходимо проверить или добавить
     * @return Проверенный и дополненный в случае необходимости словарь.
     * //todo Подумать как вместо дублирования просто вызывать перегруженный метод
     */
    protected Map<String, String> updateSettings(String filePath, String... jsonKeys) {
        if (settings == null) settings = new HashMap<>();
        for (var jsonKey:jsonKeys) {
            if (!settings.containsKey(jsonKey))
                updateJsonRow(settings, filePath, jsonKey);
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
