package ru.dogobot.Dogobot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;

@Slf4j
@Service
public class Terminaler {

    public String processExecute(String script) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        String term1 = "bash", term2 = "-c";
        if (os.contains("win")) {
            term1 = "cmd";
            term2 = "/c";
        }
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(new String[] { term1, term2, "\"\"%s\"\"".formatted(script)});

        InputStream is = pr.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder report = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            report.append(line).append("\n");
        }

        return report.toString();
    }

    public String processBuilderExecute(String[] script){
//        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        ProcessBuilder builder = new ProcessBuilder(script);

        InputStream is = null;
        try {
            is = builder.start().getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder report = new StringBuilder();
        String line;
        while (true) {
            try {
                if ((line = reader.readLine()) == null) break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            report.append(line).append("\n");
        }

        return report.toString();
    }

    protected void appCloneAndClose(){
        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String currentJar = new File(new File("."), "Dogobot-0.0.1-SNAPSHOT.jar").getAbsolutePath();

        ProcessBuilder builder = new ProcessBuilder(javaBin, "-jar", currentJar);
        try {
            builder.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.exit(0);
    }
}
