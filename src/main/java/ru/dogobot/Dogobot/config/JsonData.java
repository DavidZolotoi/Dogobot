package ru.dogobot.Dogobot.config;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class JsonData {

    private String filePath = "../.hidden/.hidden";

    private Map<String, String> settings;
    protected Map<String, String> getSettings() {
        return settings;
    }
    protected void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }

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
                mapForUpdate.put(jsonKey, readJSONFile(filePath, jsonKey));
        }
        return mapForUpdate;
    }

    /**
     * Проверить существование статического словаря настроек и ключей в нём.
     * Если словаря нет, то создать.
     * Если ключей в словаре нет, то добавить, прочитав их значения из файла.
     * @param jsonKeys ключи, которые необходимо проверить или добавить
     * @return Проверенный и дополненный в случае необходимости словарь.
     * //todo Подумать как вместо дублирования просто вызывать перегруженный метод
     */
    protected Map<String, String> updateSettings(String... jsonKeys) {
        if (settings == null) settings = new HashMap<>();
        for (var jsonKey:jsonKeys) {
            if (!settings.containsKey(jsonKey))
                settings.put(jsonKey, readJSONFile(filePath, jsonKey));
        }
        return settings;
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
            //log.problem("Файл не найден: ", filePath);
            e.printStackTrace();
        } catch (JSONException e) {
            //log.problem("Ошибка при распознавании JSON. Ключ: ", jsonKey);
            e.printStackTrace();
        } catch (IOException e) {
            //log.problem("Проблема с вводом-выводом при чтении файла: ", filePath);
            e.printStackTrace();
        }
        return jsonValue;
    }

}
