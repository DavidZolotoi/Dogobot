package ru.dogobot.Dogobot.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.dogobot.Dogobot.model.FileDir;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Component
public class FileManager {
    @Autowired
    private FileDir fileDir;

    @Getter
    private Map<String, FileDir> currentPathDict;

    /**
     * Получает полные данные о FileDir (элемент файловой системы)
     * со сканированием содержимого и данных о содержимом.
     * @param inputPath путь к элементу файловой системы
     * @return объект FileDir со всеми данными
     */
    protected FileDir getFileDirWithScan(String inputPath) {
        //сортировка и копирование только для текущей папки!!!
        this.fileDir = getFileDirWithoutScan(inputPath);
        return scanFileDirAndSaveItemData(fileDir);
    }

    /**
     * Получает первичные данные о FileDir (элемент файловой системы)
     * без сканирования содержимого и данных о содержимом.
     * @param inputPath путь к элементу файловой системы
     * @return объект FileDir
     */
    public FileDir getFileDirWithoutScan(String inputPath) {
        FileDir fileDir = new FileDir();
        fileDir.setFdJavaIoFile( new File(inputPath) );
        fileDir.setFdType( getPropertyFdType(fileDir) );
        fileDir.setFdNameOriginal( getPropertyFdNameOriginal(fileDir) );
        fileDir.setFdNameInline( getPropertyFdNameInlineButton(fileDir) );
        fileDir.setFdId( getPropertyFdId(fileDir) );
        fileDir.setFdCallbackData( getPropertyFdCallbackData(fileDir) );
        fileDir.setFdPath( getPropertyFdAbsolutePath(fileDir) );
        fileDir.setFdDate( getPropertyFdDate(fileDir) );
        fileDir.setFdSize( getPropertyFdLength(fileDir) );
        fileDir.setFdArray( getPropertyFdSortArray(fileDir) );
        return fileDir;
    }

    private FileDir.FDType getPropertyFdType(FileDir fileDir) {
        if (fileDir.getFdJavaIoFile().isFile())
            return FileDir.FDType.FILE;
        if (fileDir.getFdJavaIoFile().isDirectory())
            return FileDir.FDType.DIR;
        return FileDir.FDType.OTHER;
    }

    private String getPropertyFdNameOriginal(FileDir fileDir) {
        return fileDir.getFdJavaIoFile().getName();
    }

    /**
     * Проверяет, корректирует, в случае необходимости, и возвращает название Inline-кнопки в соответствии с требованиями.
     * Корректировка происходит путём удаления неразрешенных символов и уменьшения длины строки до максимально допустимой.
     * @param fileDir элемент файловой системы
     * @return проверенное и корректное название
     */
    public String getPropertyFdNameInlineButton(FileDir fileDir){
        //todo для поддержки других языков надо либо добавлять сюда,
        // либо менять на стратегию, чтоб в рег.выражении указать только запрещенные символы
        //регулярное выражение для допустимых символов (^ означает "всё, кроме")
        final String ALLOWED_CHARACTERS_REGEX = "[^a-zA-Zа-яА-Я0-9 .,:;`~'\"!?@#№$%^&*-_+=|<>(){}\\[\\]]";
        String nameInlineButton = fileDir.getFdNameOriginal();
        nameInlineButton = nameInlineButton.replaceAll(ALLOWED_CHARACTERS_REGEX, "");
        int maxLength = 30; //еще 2 оставляю для квадратных скобок папок []
        if (nameInlineButton.length() > maxLength) {
            int charactersToRemove = nameInlineButton.length() - maxLength + 2;
            int start = nameInlineButton.length() / 2 - charactersToRemove / 2;
            nameInlineButton = nameInlineButton.substring(0, start)
                    + ".."
                    + nameInlineButton.substring(start + charactersToRemove);
        }
        if (fileDir.getFdType() == FileDir.FDType.DIR){
            nameInlineButton = "[" + nameInlineButton + "]";
        }
        //todo можно переименовать наименование текущего элемента Array[1] в "действия над f/d"
        return nameInlineButton;
    }

    private String getPropertyFdId(FileDir fileDir) {
        return Screenshoter.getRandomStringDate();
    }

    private String getPropertyFdCallbackData(FileDir fileDir) {
        return fileDir.getFdId();
    }

    private String getPropertyFdAbsolutePath(FileDir fileDir) {
        return fileDir.getFdJavaIoFile().getAbsolutePath();
    }

    private long getPropertyFdDate(FileDir fileDir) {
        return fileDir.getFdJavaIoFile().lastModified();
    }

    private long getPropertyFdLength(FileDir fileDir) {
        return fileDir.getFdJavaIoFile().length();
    }

    /**
     * Сортирует список элементов, поднимая папки перед файлами.
     * В случае если текущий элемент - папка, то добавляет ткущую папку в начало массива с содержимым папки.
     * @return отсортированный массив элементов файловой системы, включая текущий.
     */
    private File[] getPropertyFdSortArray(FileDir fileDir) {
        File fileByNextPath = fileDir.getFdJavaIoFile();

        //массив по умолчанию
        File[] dirsAndFilesWithDefault = new File[]{
                fileByNextPath.getParentFile(),
                fileByNextPath
        };
        if (!fileDir.getFdJavaIoFile().isDirectory())   //если файл, то на этом всё
            return dirsAndFilesWithDefault;

        //сортированный массив элементов ФС внутри папки
        File[] dirsAndFilesWithoutDefault =
                Arrays.stream(fileByNextPath.listFiles())
                .sorted((file1, file2) -> {
                            if (file1.isDirectory() && !file2.isDirectory()) {
                                return -1; // Поместить папку перед файлом
                            } else if (!file1.isDirectory() && file2.isDirectory()) {
                                return 1; // Поместить файл после папки
                            } else {
                                return file1.getName().compareTo(file2.getName()); // Оставить на месте
                            }
                        })
                .toArray(File[]::new);
        //копирование - увеличение массива
        return Stream.concat(
                        Arrays.stream(dirsAndFilesWithDefault),
                        Arrays.stream(dirsAndFilesWithoutDefault)
                )
                .toArray(File[]::new);
    }

    private FileDir scanFileDirAndSaveItemData(FileDir fileDir) {
        currentPathDict = new HashMap<>();
        List<List<String>> inlineKeyboardIds = new ArrayList<>();

        for(File file : fileDir.getFdArray()) {
            FileDir fileDirInPath = getFileDirWithoutScan(file.getAbsolutePath());
            currentPathDict.put(fileDirInPath.getFdId(), fileDirInPath);

            List<String> row = new ArrayList<>();
            row.add(currentPathDict.get(fileDirInPath.getFdId()).getFdId());
            inlineKeyboardIds.add(row);
        }

        fileDir.setFdInlineKeyboardIds(
                inlineKeyboardIds
        );

        return fileDir;
    }
}


//                fileDirId = "d%d".formatted(dirCount);
