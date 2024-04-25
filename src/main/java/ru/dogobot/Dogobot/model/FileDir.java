package ru.dogobot.Dogobot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Data
@Component
public class FileDir {

    @Getter
    @AllArgsConstructor
    public enum FDType {
        FILE("Файл"),
        DIR("Папка"),
        OTHER("Неизвестно");

        private final String typeString;
    }

    private File    fdJavaIoFile;
    private String  fdId;
    private FDType  fdType;
    private String  fdNameOriginal;
    private String  fdNameInline;
    private String  fdCallbackData;
    private String  fdPath;
    private Long    fdDate;
    private Long    fdSize;

    private File[]  fdArray;
    private List<List<String>> fdInlineKeyboardIds;

    @Override
    public String toString() {
        try {
            String separator = System.lineSeparator();
            StringBuilder result = new StringBuilder().append("Информация о папке/файле: ").append(separator);
            result.append("Тип: ").append(getFdTypeStringB(separator));
            result.append("Имя: ").append(getFdNameOriginal(separator));
            result.append("Полный путь: ").append(getFdPathStringB(separator));
            result.append("Дата изменения: ").append(getFdDateStringB(separator));
            result.append("Размер: ").append(getFdSizeStringB(separator));
            result.append("Содержимое: ").append(getFdArrayStringB(separator));
            return result.toString();
        } catch (Exception e) {
            String report = "Проблема с файлом/папкой: " + e.getMessage();
            log.error(report);
            return report;
        }
    }

    /**
     * Собирает строку с содержимым папки/файла
     * @param separator системыный разделитель строк
     * @return строка с содержимым папки/файла
     */
    private StringBuilder getFdArrayStringB(String separator) {
        StringBuilder fdArrayStringB = new StringBuilder();


        //todo ДОДЕЛАТЬ - продолжить отсюда
//        if (FileDir.this.fdArray == null){
//            log.error("Проблема с содержимым папки/файла: " + separator + Arrays.toString(FileDir.this.fdArray));
//            fdArrayStringB.append("Неизвестно,").append(separator);
//            return fdArrayStringB;
//        }

        if (FileDir.this.fdType == FDType.DIR && FileDir.this.fdArray.length > 2) {
            for (int i = 2; i < FileDir.this.fdArray.length; i++) { //минуя родителя и текущий
                fdArrayStringB.append(FileDir.this.fdArray[i].getName()).append(",").append(separator);
            }
            return fdArrayStringB;
        }

        if (FileDir.this.fdType == FDType.DIR && FileDir.this.fdArray.length == 2) {
            fdArrayStringB.append(separator);
            return fdArrayStringB;
        }

        if (FileDir.this.fdType == FDType.FILE) {
            fdArrayStringB.append(separator);
            return fdArrayStringB;
        }

        log.error("Проблема с содержимым папки/файла: " + separator + Arrays.toString(FileDir.this.fdArray));
        fdArrayStringB.append("Неизвестно,").append(separator);
        return fdArrayStringB;


    }

    /**
     * Собирает строку с размером папки/файла
     * @param separator системый разделитель строк
     * @return строка с размером папки/файла
     */
    private StringBuilder getFdSizeStringB(String separator) {
        StringBuilder fdSizeStringB = new StringBuilder();
        if (FileDir.this.fdSize != null){
            fdSizeStringB.append(FileDir.this.fdSize).append(",").append(separator);
        } else {
            log.error("Проблема с размером папки/файла: " + separator + Arrays.toString(FileDir.this.fdArray));
            fdSizeStringB.append("Неизвестно,").append(separator);
        }
        return fdSizeStringB;
    }

    /**
     * Собирает строку с датой изменения папки/файла
     * @param separator системный разделитель строк
     * @return строка с датой изменения папки/файла
     */
    private StringBuilder getFdDateStringB(String separator) {
        StringBuilder fdDateStringB = new StringBuilder();
        if (FileDir.this.fdDate != null){
            String formattedDate = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(FileDir.this.fdDate);
            fdDateStringB.append("'").append(formattedDate).append("',").append(separator);
        } else {
            fdDateStringB.append("Неизвестно',").append(separator);
        }
        return fdDateStringB;
    }

    /**
     * Собирает строку с путём папки/файла
     * @param separator системный разделитель строк
     * @return строка с путём папки/файла
     */
    private StringBuilder getFdPathStringB(String separator) {
        StringBuilder fdPathStringB = new StringBuilder();
        if (FileDir.this.fdPath != null){
            fdPathStringB.append("'").append(FileDir.this.fdPath).append("',").append(separator);
        } else {
            fdPathStringB.append("Неизвестно',").append(separator);
        }
        return fdPathStringB;
    }

    /**
     * Собирает строку с именем папки/файла
     * @param separator системный разделитель строк
     * @return строка с именем папки/файла
     */
    private StringBuilder getFdNameOriginal(String separator) {
        StringBuilder getFdNameOriginalStringB = new StringBuilder();
        if (FileDir.this.fdNameOriginal != null){
            getFdNameOriginalStringB.append("'").append(FileDir.this.fdNameOriginal).append("',").append(separator);
        } else {
            getFdNameOriginalStringB.append("Неизвестно',").append(separator);
        }
        return getFdNameOriginalStringB;
    }

    /**
     * Собирает строку с типом папки/файла
     * @param separator системный разделитель строк
     * @return строка с типом папки/файла
     */
    private StringBuilder getFdTypeStringB(String separator) {
        StringBuilder fdTypeStringB = new StringBuilder();
        if (FileDir.this.fdType != null){
            fdTypeStringB.append(FileDir.this.fdType.typeString).append(",").append(separator);
        } else {
            fdTypeStringB.append("Неизвестно,").append(separator);
        }
        return fdTypeStringB;
    }
}
