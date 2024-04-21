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
        String separator = System.lineSeparator();
        StringBuilder fd = new StringBuilder();
        String formattedDate = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(fdDate);
        for (int i = 2; i < FileDir.this.fdArray.length; i++) { //минуя родителя и текущий
            fd.append(FileDir.this.fdArray[i].getName()).append(separator);
        }
        return "Тип: %s, %sНаименование: '%s', %sПолный путь: '%s', %sДата изменения: %s, %sРазмер, Байт: %d, %sСодержимое: %s%s"
                .formatted(fdType.typeString, separator,
                        fdNameOriginal, separator,
                        fdPath, separator,
                        formattedDate, separator,
                        fdSize, separator,
                        separator,fd.toString()
                );
    }
}
