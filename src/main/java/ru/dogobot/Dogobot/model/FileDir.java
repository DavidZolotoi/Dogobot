package ru.dogobot.Dogobot.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Slf4j
@Data
@Component
public class FileDir {
    public enum FDType {
        FILE,
        DIR;
    }

    private File    fdObject;
    private File[]  fdArray;
    private List<List<String>> fdInlineKeyboardNames;
    private String  fdPath;
    private FDType  fdType;
    private String  fdNameOriginal;
    private String  fdNameInline;
    private Long    fdDate;
    private Long    fdSize;
}
