package ru.dogobot.Dogobot.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.dogobot.Dogobot.model.FileDir;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

@Getter
@Slf4j
@Component
public class FileManager {
    final String MENU = "===[ МЕНЮ ]===";                               //должно быть менее 32 символов
    final String EXIT_DIR = "[ .. ]";                                   //должно быть менее 32 символов
    final static String SELECT_MENU_ITEM = "<< ВЫБЕРИТЕ ПУНКТ МЕНЮ >>"; //должно быть менее 32 символов

    @Getter  @AllArgsConstructor
    protected enum FileDirMenu {
        ALL_INFO(                   "CBD_FDM_00",   "Полная информация"),
        SEND_TO_ME_ON_TELEGRAM(     "CBD_FDM_01",   "Отправить мне в Telegram"),
        SEND_TO_FRIEND_ON_TELEGRAM( "CBD_FDM_02",   "Отправить другу в Telegram"),
        SEND_TO_ME_ON_EMAIL(        "CBD_FDM_03",   "Отправить мне на почту"),
        SEND_TO_FRIEND_ON_EMAIL(    "CBD_FDM_04",   "Отправить другу на почту"),
        PACK(                       "CBD_FDM_05",   "Архивировать рядом"),
        PACK_WITH_PASSWORD(         "CBD_FDM_06",   "Архивировать с паролем"),
        RENAME(                     "CBD_FDM_07",   "Переименовать"),
        COPY(                       "CBD_FDM_08",   "Копировать"),
        MOVE(                       "CBD_FDM_09",   "Переместить"),
        DELETE(                     "CBD_FDM_10",   "Удалить"),
        TERMINAL(                   "CBD_FDM_11",   "Терминал"),
        REMOVE_MENU(                "CBD_FDM_12",   "<< УБРАТЬ МЕНЮ >>");

        private final String buttonCallback;
        private final String buttonText;
    }

    @Autowired
    private FileDir fileDir;
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
        return scanFileDirAndSaveItemData(this.fileDir);
    }

    /**
     * Получает первичные данные о FileDir (элемент файловой системы)
     * без сканирования содержимого и данных о содержимом.
     * @param inputPath путь к элементу файловой системы
     * @return объект FileDir
     */
    protected FileDir getFileDirWithoutScan(String inputPath) {
        FileDir fileDir = new FileDir();
        fileDir.setFdJavaIoFile( new File(inputPath) );
        fileDir.setFdId( getPropertyFdId(fileDir) );
        fileDir.setFdType( getPropertyFdType(fileDir) );
        fileDir.setFdNameOriginal( getPropertyFdNameOriginal(fileDir) );
        fileDir.setFdNameInline( getPropertyFdNameInlineButton(fileDir) );
        fileDir.setFdCallbackData( getPropertyFdCallbackData(fileDir) );
        fileDir.setFdPath( getPropertyFdAbsolutePath(fileDir) );
        fileDir.setFdDate( getPropertyFdDate(fileDir) );
        fileDir.setFdSize( getPropertyFdLength(fileDir) );
        fileDir.setFdArray( getPropertyFdSortArray(fileDir) );
        return fileDir;
    }

    /**
     * Получает (создает) идентификатор для FileDir.
     * @param fileDir элемент файловой системы, для которого работает метод
     * @return идентификатор = случайная неповторимая строка на основе текущей даты и части строкового окончания.
     * Длина строки 30 символов, что вписывается в различные требования.
     */
    private String getPropertyFdId(FileDir fileDir) {
        if (fileDir.getFdId() != null) return fileDir.getFdId();
        final String txtForFinish = "%s%s".formatted(
                fileDir.getFdJavaIoFile().getAbsolutePath(), fileDir.getFdJavaIoFile().length()
        );
        return Screenshoter.getRandomStringDate(txtForFinish);
    }

    /**
     * Получает (определяет) тип для FileDir.
     * @param fileDir элемент файловой системы, для которого работает метод
     * @return тип (файл или папка) - enum FileDir.FDType
     */
    private FileDir.FDType getPropertyFdType(FileDir fileDir) {
        if (fileDir.getFdJavaIoFile().isFile())
            return FileDir.FDType.FILE;
        if (fileDir.getFdJavaIoFile().isDirectory())
            return FileDir.FDType.DIR;
        return FileDir.FDType.OTHER;
    }

    /**
     * Получает оригинальное название FileDir.
     * @param fileDir элемент файловой системы, для которого работает метод
     * @return оригинальное название
     */
    private String getPropertyFdNameOriginal(FileDir fileDir) {
        return fileDir.getFdJavaIoFile().getName();
    }

    /**
     * Проверяет и в случае необходимости корректирует оригинальное название для соответствия требованиям к InlineKeyboardButton.
     * Корректировка происходит путём удаления неразрешенных символов и уменьшения длины строки до максимально допустимой.
     * @param fileDir элемент файловой системы, для которого работает метод
     * @return проверенное и корректное название для InlineKeyboardButton
     */
    protected String getPropertyFdNameInlineButton(FileDir fileDir){
        //Для поддержки других языков надо либо добавлять сюда,
        //либо менять на стратегию, чтоб в рег. выражении указать только запрещенные символы
        //Регулярное выражение для допустимых символов (^ означает "всё, кроме")
        final String ALLOWED_CHARACTERS_REGEX = "[^a-zA-Zа-яА-Я0-9 .,:;`~'\"!?@#№$%^&*-_+=|<>(){}\\[\\]]";
        final int MAX_LENGTH = 30; //еще 2 оставляю для квадратных скобок папок []
        String nameInlineButton = fileDir.getFdNameOriginal();

        if (this.fileDir.getFdJavaIoFile() != null){
            if (fileDir.getFdJavaIoFile().equals(this.fileDir.getFdJavaIoFile().getParentFile()))
                return EXIT_DIR;
            if (fileDir.getFdJavaIoFile().equals(this.fileDir.getFdJavaIoFile()))
                return MENU;
        }

        nameInlineButton = nameInlineButton.replaceAll(ALLOWED_CHARACTERS_REGEX, "");
        if (nameInlineButton.length() > MAX_LENGTH) {
            int charactersToRemove = nameInlineButton.length() - MAX_LENGTH + 2;
            int start = nameInlineButton.length() / 2 - charactersToRemove / 2;
            nameInlineButton = nameInlineButton.substring(0, start)
                    + ".."
                    + nameInlineButton.substring(start + charactersToRemove);
        }
        if (fileDir.getFdType() == FileDir.FDType.DIR){
            nameInlineButton = "[" + nameInlineButton + "]";
        }

        return nameInlineButton;
    }

    /**
     * Получает (задаёт) команду CallbackData для InlineKeyboardButton.
     * @param fileDir элемент файловой системы, для которого работает метод
     * @return CallbackData команда = FileDir.getFdId()
     */
    private String getPropertyFdCallbackData(FileDir fileDir) {
        return fileDir.getFdId();
    }

    /**
     * Получает полный путь к FileDir.
     * @param fileDir элемент файловой системы, для которого работает метод
     * @return полный путь
     */
    private String getPropertyFdAbsolutePath(FileDir fileDir) {
        return fileDir.getFdJavaIoFile().getAbsolutePath();
    }

    /**
     * Получает дату последнего изменения FileDir.
     * @param fileDir элемент файловой системы, для которого работает метод
     * @return дата последнего изменения
     */
    private long getPropertyFdDate(FileDir fileDir) {
        return fileDir.getFdJavaIoFile().lastModified();
    }

    /**
     * Получает занимаемый размер FileDir.
     * @param fileDir элемент файловой системы, для которого работает метод
     * @return занимаемый размер
     */
    private long getPropertyFdLength(FileDir fileDir) {
        return fileDir.getFdJavaIoFile().length();
    }

    /**
     * Получает (сортирует и создаёт) список элементов.
     * Массив элементов создаётся в следующем порядке:
     * - элемент родительской папки;
     * - элемент текущей папки;
     * - отсортированные элементы внутри папки (папки перед файлами).
     * @param fileDir элемент файловой системы, для которого работает метод
     * @return отсортированный массив элементов файловой системы, включая текущий и родительский
     */
    private File[] getPropertyFdSortArray(FileDir fileDir) {
        File fileByNextPath = fileDir.getFdJavaIoFile();

        //массив по умолчанию
        File[] dirsAndFilesWithDefault = new File[]{
                fileByNextPath,
                fileByNextPath.getParentFile()
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

    /**
     * Сканирует папку и сохраняет данные о содержимом в FileDir.
     * @param fileDir элемент файловой системы, для которого работает метод
     * @return FileDir, заполненный всеми данными о содержимом
     */
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
