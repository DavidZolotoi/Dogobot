package ru.dogobot.Dogobot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Component
public class Screenshoter {

    public String take() {
        System.setProperty("java.awt.headless", "false");
        GraphicsEnvironment ge = null;
        GraphicsDevice[] screens = new GraphicsDevice[0];
        try {
            ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            screens = ge.getScreenDevices();
        } catch (HeadlessException e) {
            log.error("Не удалось получить список экранов для скриншота: " + e.getMessage());
        }

        Rectangle allScreenBounds = new Rectangle();
        for (GraphicsDevice screen : screens) {
            Rectangle screenBounds = screen.getDefaultConfiguration().getBounds();
            allScreenBounds.width += screenBounds.width;
            allScreenBounds.height = Math.max(allScreenBounds.height, screenBounds.height);
        }

        BufferedImage capture = null;
        try {
            capture = new Robot().createScreenCapture(allScreenBounds);
        } catch (AWTException e) {
            log.error("Не удалось захватить изображение для скриншота: " + e.getMessage());
        }

        String screenExtension = "png";
        String screenshotDirPath = "../screenshots";

        File dir = new File(screenshotDirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String screenPath = getScreenPath(screenExtension, screenshotDirPath);

        File imageFile = null;
        try {
            imageFile = new File(screenPath);
        } catch (Exception e) {
            log.error("Не удалось создать файл для скриншота: " + e.getMessage());
        }

        try {
            ImageIO.write(capture, screenExtension, imageFile);
        } catch (IOException e) {
            log.error("Не удалось сохранить изображение для скриншота: " + e.getMessage());
        }

        return screenPath;
    }

    private String getScreenPath(String screenExtension, String screenshotDirPath) {
        String screenName = getRandomStringDate();
        // Строка, содержащая полный путь к файлу
        return String.format("%s/%s.%s", screenshotDirPath, screenName, screenExtension);
    }

    /**
     * Генерирует случайную неповторимую строку на основе текущей даты.
     * Длина строки 29 символов, что вписывается в различные требования.
     * @return случайная неповторимая строка
     */
    protected static String getRandomStringDate() {
        var dateTimeNow = java.time.LocalDateTime.now();
        return String.format("%04d%02d%02d_%02d%02d%02d_%09d_r%02d",
                dateTimeNow.getYear(),
                dateTimeNow.getMonthValue(),
                dateTimeNow.getDayOfMonth(),
                dateTimeNow.getHour(),
                dateTimeNow.getMinute(),
                dateTimeNow.getSecond(),
                dateTimeNow.getNano(),
                new Random().nextInt(99)
        );
    }
}
