package ru.dogobot.Dogobot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

@Slf4j
@Service
public class Screenshoter {

    /**
     * Делает скриншот экрана
     * @return полный путь к скриншоту
     */
    public String take() {
        System.setProperty("java.awt.headless", "false");
        GraphicsEnvironment ge;
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

        String screenshotDirPath = "../screenshots";
        String screenName = getRandomStringDate(String.valueOf(new Random().nextInt(10)));
        String screenExtension = "png";
        String screenPath =  String.format("%s/%s.%s", screenshotDirPath, screenName, screenExtension);

        File dir = new File(screenshotDirPath);
        if (!dir.exists() && !dir.mkdirs()) {
            log.error("Не удалось создать каталог для скриншотов");
        }

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

    /**
     * Генерирует случайную неповторимую строку на основе текущей даты и части строкового окончания.
     * Длина строки 30 символов, что вписывается в различные требования.
     * @param txtForFinish текст для окончания (исходные данные, из них возьмет 6 предпоследних символов)
     * @return случайная неповторимая строка
     */
    protected static String getRandomStringDate(String txtForFinish) {
        String dateTimeNow = java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyMMdd_HHmmss_SSSSSSSSS")
                );
        if (txtForFinish.length() > 6)
            txtForFinish = txtForFinish.substring(txtForFinish.length() - 7, txtForFinish.length() - 1);
        return String.format("%s_%s",
                dateTimeNow, txtForFinish
        );
    }
}
