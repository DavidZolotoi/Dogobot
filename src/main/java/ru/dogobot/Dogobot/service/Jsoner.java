package ru.dogobot.Dogobot.service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@Slf4j
@Setter
@Getter
@Service
public class Jsoner {

    /**
     * Метод, получающий значение по ключу из JSON-файла
     * @param filePath путь к файлу JSON,
     * @param jsonKey ключ, по которому необходимо найти значение
     * @return искомое значение
     */
    public String readJSONFile(String filePath, String jsonKey) {
        String jsonValue = null;
        try (FileReader reader = new FileReader(filePath))
        {
            JSONTokener tokener = new JSONTokener(reader);
            JSONObject jsonObject = new JSONObject(tokener);
            jsonValue = jsonObject.getString(jsonKey);
        } catch (FileNotFoundException e) {
            log.warn("Файл не найден: " + filePath + System.lineSeparator() + e.getMessage());
        } catch (JSONException e) {
            log.warn("Ошибка при распознавании JSON. Ключ: " + jsonKey + System.lineSeparator() + e.getMessage());
        } catch (IOException e) {
            log.warn("Проблема с вводом-выводом при чтении файла: " + filePath + System.lineSeparator() + e.getMessage());
        }
        return jsonValue;
    }

    /**
     * Изменяет (добавляет, в случае отсутствия) значение по ключу в JSON-файле.
     * @param jsonKey ключ, по которому необходимо изменить значение
     * @param jsonValue новое значение
     * @return результат изменения
     */
    public boolean updateValueJSONFile(String filePath, int indentFactor, String jsonKey, String jsonValue) {
        boolean result = false;
        try {
            JSONObject jsonObject;

            try (FileReader reader = new FileReader(filePath)) {
                JSONTokener tokener = new JSONTokener(reader);
                jsonObject = new JSONObject(tokener);
                jsonObject.put(jsonKey, jsonValue);
            }

            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(jsonObject.toString(indentFactor));
            }

            result = true;
        } catch (FileNotFoundException e) {
            log.error("Файл не найден: " + filePath + System.lineSeparator() + e.getMessage());
        } catch (JSONException e) {
            log.error("Ошибка при распознавании JSON. Ключ: " + jsonKey + System.lineSeparator() + e.getMessage());
        } catch (IOException e) {
            log.error("Проблема с вводом-выводом при изменении файла: " + filePath + System.lineSeparator() + e.getMessage());
        }

        return result;
    }

}
