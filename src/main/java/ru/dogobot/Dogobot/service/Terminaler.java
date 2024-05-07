package ru.dogobot.Dogobot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Arrays;

@Slf4j
@Service
public class Terminaler {

    /**
     * Запускает команду(ы) в терминале
     * @param script команда(ы) для запуска в терминале
     * @return процесс запуска команды
     * @throws IOException если возникли исключения
     */
    public Process processBuilderExecute(String script) throws IOException {
        String[] bashOrCmdAndScript = fillCommandForOS(script);
        ProcessBuilder builderForScript = new ProcessBuilder(bashOrCmdAndScript);
        log.info("Running command: " + Arrays.toString(bashOrCmdAndScript));
        return builderForScript.start();
    }

    /**
     * Заполняет команду для запуска в терминале с учетом ОС
     * @param script команда для запуска
     * @return команду для запуска в необходимом формате (String[])
     */
    private String[] fillCommandForOS(String script) {
        String bashOrCmd1 = "bash";
        String bashOrCmd2 = "-c";
        if (System.getProperty("os.name").toLowerCase().contains("win")){
            bashOrCmd1 = "cmd";
            bashOrCmd2 = "/c";
        }
        script = script.replace("—", "--");
        return new String[]{bashOrCmd1, bashOrCmd2, script};
    }


    private StringBuilder getAnswerFromProcessStream(Process processForScript) throws IOException {
        StringBuilder report = new StringBuilder();
        InputStream is = processForScript.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = reader.readLine()) != null) {
            report.append(line).append("\n");
        }
        return report;
    }

    /**
     * Запускает команду(ы) в терминале и возвращает ответ
     * @param script команда(ы) для запуска в терминале
     * @return ответ от команды в полученном процессе
     * @throws IOException если возникли исключения
     */
    public String processBuilderExecuteWithAnswer(String script) throws IOException {
        Process processForScript = processBuilderExecute(script);
        return getAnswerFromProcessStream(processForScript).toString();
    }

    /**
     * Запускает команду(ы) в терминале и возвращает ответ - второй запасной способ
     * @param script команда(ы) для запуска в терминале
     * @return ответ команды
     * @throws IOException если возникли исключения
     */
    public String processExecuteWithAnswer(String script) throws IOException {
        String[] bashOrCmdAndScript = fillCommandForOS(script);
        Runtime rt = Runtime.getRuntime();
        Process processForScript = rt.exec(bashOrCmdAndScript);
        return getAnswerFromProcessStream(processForScript).toString();
    }

}
