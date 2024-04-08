package ru.dogobot.Dogobot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Slf4j
@Data
@Component
public class FileDir {
    @Getter
    @AllArgsConstructor
    public enum FDType {
        FILE("f"),
        DIR("d"),
        OTHER("o");

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


}
