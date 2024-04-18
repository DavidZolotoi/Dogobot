package ru.dogobot.Dogobot.service;

import com.vdurmont.emoji.EmojiParser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.mail.EmailException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.dogobot.Dogobot.config.EmailConfig;
import ru.dogobot.Dogobot.model.FileDir;
import ru.dogobot.Dogobot.model.User;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Stream;

@Getter
@Slf4j
@Service
public class FileManager {
    final String MENU = "===[ МЕНЮ ]===";                               //должно быть менее 32 символов
    final String EXIT_DIR = "[ .. ]";                                   //должно быть менее 32 символов
    final static String SELECT_MENU_ITEM = "<< ВЫБЕРИТЕ ПУНКТ МЕНЮ >>"; //должно быть менее 32 символов

    @Getter  @AllArgsConstructor
    protected enum FileDirMenu {
        GET_INFO(            "CBD_FDM_00", "Получить информацию"),
        GET_ON_TELEGRAM(     "CBD_FDM_01", "Получить в Telegram"),
        GET_ON_EMAIL(        "CBD_FDM_10", "Получить на почту"),
        SEND_TO_EMAIL(       "CBD_FDM_11", "Отправить на почту"),
        PACK(                "CBD_FDM_20", "Упаковать в zip"),
        PACK_WITH_PASSWORD(  "CBD_FDM_21", "Упаковать в zip с паролем"),
        UNPACK(              "CBD_FDM_22", "Распаковать из zip"),
        UNPACK_WITH_PASSWORD("CBD_FDM_23", "Распаковать из zip с паролем"),
        RENAME(              "CBD_FDM_30", "Переименовать"),
        MOVE(                "CBD_FDM_31", "Переместить"),
        COPY(                "CBD_FDM_32", "Копировать"),
        DELETE(              "CBD_FDM_33", "Удалить"),
        TERMINAL(            "CBD_FDM_40", "Терминал"),
        REMOVE_MENU(         "CBD_FDM_99", "<< УБРАТЬ МЕНЮ >>");
        //SEND_TO_TELEGRAM( "CBD_FDM_02", "Отправить другу в Telegram");

        private final String buttonCallback;
        private final String buttonText;

    }

    @Autowired
    Userer userer;

    @Autowired
    Screenshoter screenshoter;

    @Autowired
    Emailer emailer;
    @Autowired
    EmailConfig emailConfig;

    @Autowired
    Archiver archiver;

    @Autowired
    Filer filer;

    @Autowired
    Terminaler terminaler;

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
        if (fileByNextPath.listFiles() == null)   //если файл, то на этом всё
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
            if ( file == null)
                continue;
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

    //region РАБОТА С ПОЛЬЗОВАТЕЛЯМИ

    public User createUserFromUpdateWithoutDB(Update update) {
        User user = new User();
        var message = update.getMessage();
        user.setChatId(message.getFrom().getId());
        user.setFirstName(message.getChat().getFirstName());
        user.setLastName(message.getChat().getLastName());
        user.setUserName(message.getChat().getUserName());
        user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
        user.setPackPassword(userer.getUserConfig().getPackPassword());
        user.setPersonalEmail(emailConfig.getEmailFrom());
        return user;
    }

    public String findUserOrRegister(Update update) {
        String smileBlush = EmojiParser.parseToUnicode(":blush:");
        String report = null;

        if(findUser(update) != null){
            report = "Данные о пользователе найдены. Вы можете проверить их корректность вызвав соответствующую команду из меню" + smileBlush;
        } else{
            registerUser(update);
            report = "Пользователь успешно зарегистрирован!" + smileBlush;
        }

        return report;
    }

    public User findUser(Update update) {
        String smileBlush = EmojiParser.parseToUnicode(":blush:");
        String report = null;
        User user = null;

        try {
            if (update.getMessage() != null)
                user = userer.findUserById(update.getMessage().getChatId());
            if (update.getCallbackQuery() != null)
                user = userer.findUserById(update.getCallbackQuery().getFrom().getId());
            report = "Данные о пользователе найдены " + smileBlush;
            log.info(report + System.lineSeparator() + user);
        } catch (Exception e) {
            report = "Не удалось получить данные о пользователе с id: " + update.getMessage().getChatId() + ". Возможно пользователь не зарегистрирован.";
            log.error(report + System.lineSeparator() + e.getMessage());
        }

        return user;
    }

    public User registerUser(Update update) {
        String smileBlush = EmojiParser.parseToUnicode(":blush:");
        String report = null;
        User user = null;

        try {
            user = userer.registerUser(createUserFromUpdateWithoutDB(update));
            report = "Пользователь успешно зарегистрирован!" + smileBlush;
            log.info(report + System.lineSeparator() + user);
        } catch (Exception e) {
            report = "Не удалось зарегистрировать пользователя.";
            log.error(report + System.lineSeparator() + e.getMessage());
            //todo в теории везде, где ошибка, надо отчитаться пользователю, но не факт что прямо в этом методе
        }

        return user;
    }

    public User deleteUser(Update update) {
        String report = null;
        User user = findUser(update);
        if(user != null){
            try {
                user = userer.deleteUser(user);
                report = "Данные о пользователе удалены." + System.lineSeparator() + user.toString();
                log.info(report);
            } catch (Exception e) {
                report = "Не удалось удалить данные о пользователе.";
                log.error(report);
            }
        } else{
            report = "Данных о пользователе не найдено.";
            log.error(report);
        }

        return user;
    }

    public User updatePackPassword(Update update, String newPackPassword) {
        String report = null;
        User user = findUser(update);
        if(user != null){
            try {
                user = userer.updatePackPassword(user, newPackPassword);
                report = "Установлен новый пароль для пользователя. " + System.lineSeparator() + user.toString();
                log.info(report);
            } catch (Exception e) {
                report = "Не удалось установить пароль для пользователя.";
                log.error(report);
            }
        } else{
            report = "Данных о пользователе не найдено.";
            log.error(report);
        }

        return user;
    }

    public User updateOtherMail(Update update, String newOtherMail) {
        String report = null;
        User user = findUser(update);
        if(user != null){
            try {
                user = userer.updateOtherMail(user, newOtherMail);
                report = "Установлен новый 'другой адрес электронной почты (для отправки на него писем)'. " + System.lineSeparator() + user.toString();
                log.info(report);
            } catch (Exception e) {
                report = "Не удалось установить новый 'другой адрес электронной почты (для отправки на него писем)'.";
                log.error(report);
            }
        } else{
            report = "Данных о пользователе не найдено.";
            log.error(report);
        }

        return user;
    }


    //endregion


    //region РАБОТА С ЭЛЕКТРОННОЙ ПОЧТОЙ

    /**
     * Отправляет письмо с вложением на почту из настроек.
     * @param fileDir элемент файловой системы, для которого работает метод
     * @param recipient адрес получателя из настроек
     */
    protected void sendEmailWithAttachment(FileDir fileDir, String recipient) {
        String pathToAttachment = fileDir.getFdPath();
        String subject = (pathToAttachment.length() > 30)
                ?
                "....%s".formatted(pathToAttachment.substring(pathToAttachment.length() - 30))
                :
                pathToAttachment;
        String text = fileDir.toString();
        try {
            emailer.sendEmailWithAttachment(recipient, subject, text, pathToAttachment);

            //emailer.receiveEmailWithAttachment("......./");
        } catch (EmailException e) {
            log.error("Не удалось отправить письмо: " + e.getMessage());
        }
    }

    //endregion

    //region РАБОТА С АРХИВАМИ

    public void zipFileDirWithoutPassword(FileDir sourceFileDir) {
        //todo подготовить отчет для отправки пользователю и в лог
        archiver.zipFolderWithoutPassword(sourceFileDir.getFdPath());
    }

    public void zipFileDirWithPassword(FileDir sourceFileDir, String password) {
        //todo подготовить отчет для отправки пользователю и в лог
        archiver.zipFolderWithPassword(sourceFileDir.getFdPath(), password);
    }

    public void unzipFileDirWithoutPassword(FileDir sourceFileDir) {
        //todo подготовить отчет для отправки пользователю и в лог
        archiver.unzipFileWithoutPassword(sourceFileDir.getFdPath());
    }

    public void unzipFileDirWithPassword(FileDir sourceFileDir, String password) {
        //todo подготовить отчет для отправки пользователю и в лог
        archiver.unzipFileWithPassword(sourceFileDir.getFdPath(), password);
    }

    //endregion

    //region РАБОТА С ФАЙЛАМИ

    public String fileDirRename(FileDir oldFileDir, String newName) {
        String report = "Папка или файл: " + oldFileDir.getFdNameOriginal() + " переименован в: " + newName;
        if(!filer.renameFileDir(oldFileDir.getFdJavaIoFile(), newName)){
            report = "Не удалось переименовать папку или файл: " + oldFileDir.getFdNameOriginal();
            log.error(report);
        }
        return report;
    }

    public String fileDirMove(FileDir fileDire, String newPathParent) {
        String report = "Папка или файл: " + fileDire.getFdPath() + " перемещена на путь: " + newPathParent;
        if(!filer.moveFileDir(
                fileDire.getFdJavaIoFile(),
                new File(newPathParent, fileDire.getFdNameOriginal()).getAbsolutePath()
        )){
            report = "Не удалось переместить папку или файл: " + fileDire.getFdPath();
            log.error(report);
        }
        return report;
    }

    public String fileDirCopy(FileDir fileDire, String newPathParent) {
        String report = null;

        try {
            filer.copyFileDir(
                    fileDire.getFdJavaIoFile().toPath(),
                    (new File(newPathParent, fileDire.getFdNameOriginal())).toPath()
            );
            report = "Папка или файл: " + fileDire.getFdPath() + " скопирована на путь: " + newPathParent;
        } catch (Exception e){
            report = "Не удалось скопировать папку или файл: " + fileDire.getFdPath() + e.getMessage();
            log.error(report);
        }
        return report;
    }

    public String fileDirDelete(FileDir fileDir) {
        String report = null;
        try {
            filer.deleteFileDir(fileDir.getFdJavaIoFile());
            report = "Папка или файл: " + fileDir.getFdPath() + " удалена";
        } catch (Exception e){
            report = "Не удалось удалить папку или файл: " + fileDir.getFdPath() + e.getMessage();
            log.error(report);
        }
        return report;
    }

    //endregion

    protected String terminalExecute(String script) {
        String report = null;
        try {
            report = terminaler.processExecute(script);
        } catch (IOException e) {
            report = "Не удалось выполнить команду: " + script + "\n" + e.getMessage();
            log.error(report);
        }
        return report;
    }

}

