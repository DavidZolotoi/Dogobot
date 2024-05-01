package ru.dogobot.Dogobot.service;

import com.vdurmont.emoji.EmojiParser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.dogobot.Dogobot.exception.EmailerException;
import ru.dogobot.Dogobot.exception.FilerException;
import ru.dogobot.Dogobot.model.FileDir;
import ru.dogobot.Dogobot.model.User;

import java.io.File;
import java.io.FileNotFoundException;
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

    @Getter
    @AllArgsConstructor
    protected enum FileDirMenu {
        GET_INFO("CBD_FDM_00", "Получить информацию"),
        GET_ON_TELEGRAM("CBD_FDM_01", "Получить в Telegram"),
        GET_ON_EMAIL("CBD_FDM_10", "Получить на почту"),
        SEND_TO_EMAIL("CBD_FDM_11", "Отправить на почту"),
        PACK("CBD_FDM_20", "Упаковать в zip"),
        PACK_WITH_PASSWORD("CBD_FDM_21", "Упаковать в zip с паролем"),
        UNPACK("CBD_FDM_22", "Распаковать из zip"),
        UNPACK_WITH_PASSWORD("CBD_FDM_23", "Распаковать из zip с паролем"),
        RENAME("CBD_FDM_30", "Переименовать"),
        MOVE("CBD_FDM_31", "Переместить"),
        COPY("CBD_FDM_32", "Копировать"),
        DELETE("CBD_FDM_33", "Удалить"),
        TERMINAL("CBD_FDM_40", "Терминал"),
        REMOVE_MENU("CBD_FDM_99", "<< УБРАТЬ МЕНЮ >>");
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
        try{
            this.fileDir = getFileDirWithoutScan(inputPath);
            return scanFileDirAndSaveItemData(this.fileDir);
        } catch (Exception e) {
            log.error("Полученный путь '" + inputPath + "' некорректен. Следом - открытие домашнего каталога."
                    + System.lineSeparator() + e.getMessage());
            return getFileDirHomeWithScan();
        }
    }

    /**
     * Получает полные данные о FileDir (элемент файловой системы) домашней папки
     * со сканированием содержимого и данных о содержимом.
     * @return объект FileDir со всеми данными
     */
    protected FileDir getFileDirHomeWithScan() {
        try{
            this.fileDir = getFileDirWithoutScan(System.getProperty("user.home") + "/forTest"); //todo убрать forTest
            return scanFileDirAndSaveItemData(this.fileDir);
        } catch (Exception e) {
            log.error("Не открывается даже домашняя папка" + System.lineSeparator() + e.getMessage());
            return null;
        }
    }

    /**
     * Получает некоторые данные о FileDir (элемент файловой системы)
     * без сканирования содержимого и данных о содержимом.
     * @param inputPath путь к элементу файловой системы
     * @return объект FileDir
     */
    protected FileDir getFileDirWithoutScan(String inputPath) {
        FileDir fileDir = new FileDir();
        fileDir.setFdJavaIoFile(new File(inputPath));
        fileDir.setFdId(getPropertyFdId(fileDir));
        fileDir.setFdType(getPropertyFdType(fileDir));
        fileDir.setFdNameOriginal(getPropertyFdNameOriginal(fileDir));
        fileDir.setFdNameInline(getPropertyFdNameInlineButton(fileDir));
        fileDir.setFdCallbackData(getPropertyFdCallbackData(fileDir));
        fileDir.setFdPath(getPropertyFdAbsolutePath(fileDir));
        fileDir.setFdDate(getPropertyFdDate(fileDir));
        fileDir.setFdSize(getPropertyFdLength(fileDir));
        fileDir.setFdArray(getPropertyFdSortArray(fileDir));

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
    protected String getPropertyFdNameInlineButton(FileDir fileDir) {
        //Для поддержки других языков надо либо добавлять сюда,
        //либо менять на стратегию, чтоб в рег.выражении указать только запрещенные символы
        //Регулярное выражение для допустимых символов (^ означает "всё, кроме")
        final String ALLOWED_CHARACTERS_REGEX = "[^a-zA-Zа-яА-Я0-9 .,:;`~'\"!?@#№$%^&*-_+=|<>(){}\\[\\]]";
        final int MAX_LENGTH = 30; //еще 2 оставляю для квадратных скобок папок []
        String nameInlineButton = fileDir.getFdNameOriginal();

        if (this.fileDir.getFdJavaIoFile() != null) {
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
        if (fileDir.getFdType() == FileDir.FDType.DIR) {
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
        if (fileByNextPath.listFiles() == null)
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

        for (File file : fileDir.getFdArray()) {
            if (file == null)
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

    /**
     * Получает ID чата из объекта обновления (как Message, так и CallbackQuery).
     * @param update объект обновления
     * @return ID чата
     */
    private Long getChatIdFromUpdate(Update update) {
        Long chatId = null;
        if (update.getMessage() != null) {
            chatId = update.getMessage().getChatId();
        }
        if (update.getCallbackQuery() != null) {
            chatId = update.getCallbackQuery().getFrom().getId();
        }
        return chatId;
    }

    /**
     * Поиск пользователя в БД или его регистрация в случае отсутствия.
     * @param update объект обновления
     * @return результат поиска
     */
    public String findUserOrRegister(Update update) {
        String smileBlush = EmojiParser.parseToUnicode(":blush:");
        String report;
        try {
            User user = userer.findUserById(getChatIdFromUpdate(update));
            report = "Пользователь " + user.getUserName() + " уже зарегистрирован. Вы можете проверить корректность данных, вызвав соответствующую команду из меню" + smileBlush;
            log.info(report);
        } catch (Exception e) {
            try {
                User user = createAndRegisterUser(update);
                report = "Пользователь " + user.getUserName() + " успешно зарегистрирован!" + smileBlush;
            } catch (Exception ex) {
                report = "При регистрации произошла ошибка. Пользователь не зарегистрирован или есть проблемы с БД." + smileBlush;
                log.error(report + System.lineSeparator() + ex.getMessage());
            }
        }
        return report;
    }

    /**
     * Создание и регистрация пользователя в БД.
     * @param update объект обновления
     * @return пользователь
     */
    public User createAndRegisterUser(Update update) {
        String smileBlush = EmojiParser.parseToUnicode(":blush:");
        String report;
        var message = update.getMessage();

        User user = new User(
                message.getFrom().getId(),
                message.getChat().getFirstName(),
                message.getChat().getLastName(),
                message.getChat().getUserName(),
                new Timestamp(System.currentTimeMillis()),
                userer.getUserConfig().getPackPassword(),
                userer.getUserConfig().getPersonalEmail(),
                userer.getUserConfig().getOtherEmail()
        );

        try {
            user = userer.registerUser(user);
            report = "Пользователь успешно зарегистрирован!" + smileBlush;
            log.info(report + System.lineSeparator() + user);
        } catch (Exception e) {
            report = "Не удалось зарегистрировать пользователя.";
            log.error(report + System.lineSeparator() + e.getMessage());
        }

        return user;
    }

    /**
     * Получает информацию о пользователе из БД.
     * @param update объект обновления
     * @return информация о пользователе
     */
    public String getUserInfo(Update update) {
        String report;
        User user;
        try {
            String smileBlush = EmojiParser.parseToUnicode(":blush:");
            user = userer.findUserById(getChatIdFromUpdate(update));
            report = "Данные о пользователе найдены " + smileBlush + System.lineSeparator() + user;
            log.info(report);
        } catch (Exception e) {
            report = "Не удалось получить данные о пользователе. Возможно пользователь не зарегистрирован или есть проблемы с БД.";
            log.error(report + System.lineSeparator() + e.getMessage());
        }

        return report;
    }

    /**
     * Удаляет пользователя из БД
     * @param update объект обновления
     * @return пользователь
     */
    public String deleteUser(Update update) {
        String report;
        try {
            userer.deleteUser(getChatIdFromUpdate(update));
            report = "Данные о пользователе удалены из БД.";
            log.info(report);
        } catch (Exception e) {
            report = "Не удалось удалить данные о пользователе. Возможно пользователь не зарегистрирован или есть проблемы с БД.";
            log.error(report + System.lineSeparator() + e.getMessage());
        }
        return report;
    }

    /**
     * Обновляет пароль упаковки/распаковки
     * @param update объект обновления
     * @param newPackPassword новый пароль
     * @return пользователь с обновленным паролем
     */
    public String updatePackPassword(Update update, String newPackPassword) {
        String report;
        User user;
        try {
            user = userer.updatePackPassword(getChatIdFromUpdate(update), newPackPassword);
            report = "Пароль упаковки/распаковки успешно обновлен." + System.lineSeparator() + user.getPackPassword();
            log.info(report);
        } catch (Exception e) {
            report = "При попытке обновления пароля упаковки/распаковки для пользователя возникло исключение.";
            log.error(report + System.lineSeparator() + e.getMessage());
        }
        return report;
    }

    /**
     * Обновляет персональный адрес электронной почты
     * @param update объект обновления
     * @param newPersonalMail новый персональный адрес
     * @return пользователь с обновленным персональным адресом
     */
    public String updatePersonalMail(Update update, String newPersonalMail) {
        String report;
        User user;
        try {
            user = userer.updatePersonalEmail(getChatIdFromUpdate(update), newPersonalMail);
            report = "Персональный адрес электронной почты успешно обновлен." + System.lineSeparator() + user.getPersonalEmail();
            log.info(report);
        } catch (Exception e) {
            report = "При попытке обновления персонального адреса электронной почты для пользователя возникло исключение.";
            log.error(report + System.lineSeparator() + e.getMessage());
        }
        return report;
    }

    /**
     * Обновляет другой адрес электронной почты
     * @param update объект обновления
     * @param newOtherMail новый другой адрес
     * @return пользователь с обновленным другой адрес
     */
    public String updateOtherMail(Update update, String newOtherMail) {
        String report;
        User user;
        try {
            user = userer.updateOtherEmail(getChatIdFromUpdate(update), newOtherMail);
            report = "Другой адрес электронной почты успешно обновлен." + System.lineSeparator() + user.getOtherEmail();
            log.info(report);
        } catch (Exception e) {
            report = "При попытке обновления другого адреса электронной почты для пользователя возникло исключение.";
            log.error(report + System.lineSeparator() + e.getMessage());
        }
        return report;
    }

    /**
     * Получает личные настройки пользователя
     * @param update объект обновления
     * @return строку с личными настройками
     */
    protected String getUserSettings(Update update) {
        String report;
        String sep = System.lineSeparator();
        try {
            User user = userer.findUserById(getChatIdFromUpdate(update));
            report = "Личные настройки:" + sep +
                    "---" + sep +
                    "Пароль (упаковка, распаковка и т.п.): " + user.getPackPassword() + sep +
                    "Личная почта (для получения на неё писем): " + user.getPersonalEmail() + sep +
                    "Другая почта (для отправки на неё писем): " + user.getOtherEmail() + sep;
            log.info("Личные настройки пользователя получены." + System.lineSeparator());
        } catch (Exception e) {
            report = "Не удалось получить личные настройки.";
            log.error(report + sep + e.getMessage());
        }
        return report;
    }

    //endregion

    /**
     * Делает скриншот
     * @return путь к скриншоту
     */
    protected String printScreen() {
        try {
            String screenPath = getScreenshoter().take();
            log.info("Скриншот сделан. Путь: " + screenPath);
            return screenPath;
        } catch (Exception e) {
            log.warn("Не удалось сделать скриншот. " + e.getMessage());
            return null;
        }
    }

    //region РАБОТА С ЭЛЕКТРОННОЙ ПОЧТОЙ

    /**
     * Отправляет письмо по электронной почте с вложением fileDir
     * @param fileDir элемент файловой системы, для отправки
     * @param recipient адрес получателя
     * @return отчёт отправки
     */
    protected String sendEmailWithAttachment(FileDir fileDir, String recipient) {
        String report;
        try {
            if (!fileDir.getFdJavaIoFile().exists()) {
                throw new FileNotFoundException();
            }

            if (!fileDir.getFdType().equals(FileDir.FDType.FILE)) {
                throw new EmailerException("Элемент файловой системы не является файлом. Если это папка, то перед отправкой, её стоит упаковать в архив.");
            }

            String pathToAttachment = fileDir.getFdPath();
            String subject = (pathToAttachment.length() > 30) ?
                    "....%s".formatted(pathToAttachment.substring(pathToAttachment.length() - 30))
                    : pathToAttachment;
            String text = fileDir.toString();
            report = "Отправка письма с вложением прошла без исключений." + System.lineSeparator()
                    + "Получатель: " + emailer.sendEmailWithAttachment(recipient, subject, text, pathToAttachment);
            log.info(report);
        } catch (FileNotFoundException e) {
            report = "Не удалось отправить письмо с вложением. Файл не существует. ";
            log.error(report + System.lineSeparator() + e.getMessage());
        } catch (EmailerException e) {
            report = "Не удалось отправить письмо с вложением. " + System.lineSeparator() + e.getMessage();
            log.error(report);
        } catch (Exception e) {
            report = "Не удалось отправить письмо с вложением. Проверьте корректность параметров.";
            log.error(report + System.lineSeparator() + e.getMessage());
        }
        return report;
    }

    /**
     * Отправляет письмо с вложением на личную почту
     * @param fileDir элемент файловой системы, для отправки
     * @param update  объект обновления
     * @return отчёт отправки
     */
    protected String sendEmailPersonal(FileDir fileDir, Update update) {
        String report;
        try {
            String recipient = userer.findUserById(getChatIdFromUpdate(update)).getPersonalEmail();
            report = sendEmailWithAttachment(fileDir, recipient);
        } catch (Exception e) {
            report = "Не удалось отправить письмо с вложением (или получить адрес личной почты). ";
            log.error(report + System.lineSeparator() + e.getMessage());
        }
        return report;
    }

    /**
     * Отправляет письмо с вложением на другую почту
     * @param fileDir элемент файловой системы, для отправки
     * @param update  объект обновления
     * @return отчёт отправки
     */
    protected String sendEmailOther(FileDir fileDir, Update update) {
        String report;
        try {
            String recipient = userer.findUserById(getChatIdFromUpdate(update)).getOtherEmail();
            report = sendEmailWithAttachment(fileDir, recipient);
        } catch (Exception e) {
            report = "Не удалось отправить письмо с вложением (или получить адрес другой почты). ";
            log.error(report + System.lineSeparator() + e.getMessage());
        }
        return report;
    }

    //endregion

    //region РАБОТА С АРХИВАМИ

    /**
     * Упаковывает файл или папку (метод без пароля)
     * @param sourceFileDir элемент файловой системы, для которого работает метод
     * @return отчет об упаковке
     */
    public String zipFileDirWithoutPassword(FileDir sourceFileDir) {
        String report;
        try {
            report = "Упаковка файла или папки (метод без пароля) прошла без исключений. " + System.lineSeparator()
                    + archiver.zipFolderWithoutPassword(sourceFileDir.getFdPath());
            log.info(report);
        } catch (Exception e) {
            report = "Не удалось упаковать файл или папку (метод без пароля): " + System.lineSeparator()
                    + sourceFileDir.getFdPath();
            log.error(report + System.lineSeparator() + e.getMessage());
        }
        return report;
    }

    /**
     * Упаковывает файл или папку (метод с паролем).
     * @param sourceFileDir элемент файловой системы, для которого работает метод
     * @param password      пароль для упаковки
     * @return отчет об упаковке
     */
    public String zipFileDirWithPassword(FileDir sourceFileDir, String password) {
        String report;
        try {
            report = "Упаковка файла или папки (метод с паролем) прошла без исключений. " + System.lineSeparator()
                    + archiver.zipFolderWithPassword(sourceFileDir.getFdPath(), password);
            log.info(report);
        } catch (Exception e) {
            report = "Не удалось упаковать файл или папку (метод без пароля): " + System.lineSeparator()
                    + sourceFileDir.getFdPath();
            log.error(report + System.lineSeparator() + e.getMessage());
        }
        return report;
    }

    /**
     * Упаковывает файл или папку (метод с паролем - перегрузка без указания пароля).
     * @param sourceFileDir элемент файловой системы, для которого работает метод
     * @param update        объект обновления
     * @return отчет об упаковке
     */
    public String zipFileDirWithPassword(FileDir sourceFileDir, Update update) {
        String report;
        try {
            String password = userer.findUserById(getChatIdFromUpdate(update)).getPackPassword();
            report = zipFileDirWithPassword(sourceFileDir, password);
        } catch (Exception e) {
            report = "Не удалось получить пароль или не удалось упаковать файл/папку (метод с паролем - перегрузка без указания пароля): " + System.lineSeparator()
                    + sourceFileDir.getFdPath();
            log.error(report + System.lineSeparator() + e.getMessage());
        }
        return report;
    }

    /**
     * Распаковывает файл или папку (метод без пароля).
     * Если в родительской папке архива есть файлы или папки с именами, как в архиве, то они будут заменены.
     * @param sourceFileDir элемент файловой системы, для которого работает метод.
     * @return отчет о распаковке
     */
    public String unzipFileDirWithoutPassword(FileDir sourceFileDir) {
        String report;
        try {
            report = "Распаковка файла или папки (метод без пароля) прошла без исключений. " + System.lineSeparator()
                    + archiver.unzipFileWithoutPassword(sourceFileDir.getFdPath()) + System.lineSeparator()
                    + "Обратите внимание, если в родительской папке архива '" + sourceFileDir.getFdPath() + "' были файлы или папки с именами, как в распакованном архиве, то они были заменены на распакованные.";
            log.info(report);
        } catch (Exception e) {
            report = "Не удалось распаковать файл или папку (метод без пароля): " + System.lineSeparator()
                    + sourceFileDir.getFdPath();
            log.error(report + System.lineSeparator() + e.getMessage());
        }
        return report;
    }

    /**
     * Распаковывает файл или папку (метод с паролем).
     * Если в родительской папке архива есть файлы или папки с именами, как в архиве, то они будут заменены.
     * @param sourceFileDir элемент файловой системы, для которого работает метод
     * @param password      пароль для распаковки
     * @return отчет о распаковке
     */
    public String unzipFileDirWithPassword(FileDir sourceFileDir, String password) {
        String report;
        try {
            report = "Распаковка файла или папки (метод с паролем) прошла без исключений. " + System.lineSeparator()
                    + archiver.unzipFileWithPassword(sourceFileDir.getFdPath(), password) + System.lineSeparator()
                    + "Обратите внимание, если в родительской папке архива '" + sourceFileDir.getFdPath() + "' были файлы или папки с именами, как в распакованном архиве, то они были заменены на распакованные.";
            log.info(report);
        } catch (Exception e) {
            report = "Не удалось распаковать файл или папку (метод с паролем): " + System.lineSeparator()
                    + sourceFileDir.getFdPath();
            log.error(report + System.lineSeparator() + e.getMessage());
        }
        return report;
    }

    /**
     * Распаковывает файл или папку (метод с паролем - перегрузка без указания пароля).
     * Если в родительской папке архива есть файлы или папки с именами, как в архиве, то они будут заменены.
     * @param sourceFileDir элемент файловой системы, для которого работает метод
     * @param update        объект обновления
     * @return отчет о распаковке
     */
    public String unzipFileDirWithPassword(FileDir sourceFileDir, Update update) {
        String report;
        try {
            String password = userer.findUserById(getChatIdFromUpdate(update)).getPackPassword();
            report = unzipFileDirWithPassword(sourceFileDir, password);
        } catch (Exception e) {
            report = "Не удалось получить пароль или не удалось распаковать файл/папку (метод с паролем - перегрузка без указания пароля): " + System.lineSeparator()
                    + sourceFileDir.getFdPath();
            log.error(report + System.lineSeparator() + e.getMessage());
        }
        return report;
    }

    //endregion

    //region РАБОТА С ФАЙЛАМИ

    /**
     * Переименовывает папку или файл
     * @param oldFileDir элемент файловой системы для которого работает метод
     * @param newName новое имя
     * @return отчет о переименовании
     */
    public String fileDirRename(FileDir oldFileDir, String newName) {
        String report;
        try {
            File newFile = filer.renameFileDir(oldFileDir.getFdJavaIoFile(), newName);
            report = "Папка или файл: " + oldFileDir.getFdNameOriginal() + " переименован в: " + newFile.getName();
            log.info(report);
        } catch (FilerException e) {
            report = "Не удалось переименовать папку или файл. Подробности:" + System.lineSeparator() + e.getMessage();
            log.error(report);
        } catch (Exception e) {
            report = "Не удалось переименовать папку или файл.";
            log.error(report + " Подробности:" + System.lineSeparator() + e.getMessage());
        }
        return report;
    }

    /**
     * Перемещает папку или файл
     * @param oldFileDir элемент файловой системы для которого работает метод
     * @param newPathParent путь к родительской папке нового места
     * @return отчет о перемещении
     */
    public String fileDirMove(FileDir oldFileDir, String newPathParent) {
        String report;
        try {
            File newFile = filer.moveFileDir(
                    oldFileDir.getFdJavaIoFile(),
                    new File(newPathParent, oldFileDir.getFdNameOriginal()).getAbsolutePath()
            );
            report = "Папка или файл: " + oldFileDir.getFdPath() + " перемещен в: " + newFile.getAbsolutePath();
            log.info(report);
        } catch (FilerException e) {
            report = "Не удалось переместить папку или файл. Подробности:" + System.lineSeparator() + e.getMessage();
            log.error(report);
        } catch (Exception e) {
            report = "Не удалось переименовать папку или файл.";
            log.error(report + " Подробности:" + System.lineSeparator() + e.getMessage());
        }
        return report;
    }

    /**
     * Копирует папку или файл
     * @param fileDire элемент файловой системы для которого работает метод
     * @param newPathParent путь к родительской папке нового места
     * @return отчет о копировании
     */
    public String fileDirCopy(FileDir fileDire, String newPathParent) {
        String report;
        File newFile;
        try {
            newFile = filer.copyFileDir(
                    fileDire.getFdJavaIoFile().toPath(),
                    (new File(newPathParent, fileDire.getFdNameOriginal())).toPath()
            );
            report = "Папка или файл: " + fileDire.getFdPath() + " скопирована в: " + newPathParent;
            log.info(report);
        } catch (FilerException e) {
            report = "Копирование аварийно остановлено. Причина - не удалось скопировать папку или файл. Подробности:" + System.lineSeparator() + e.getMessage();
            log.error(report);
        } catch (Exception e) {
            report = "Копирование аварийно остановлено. Причина - не удалось скопировать папку или файл: " + fileDire.getFdPath();
            log.error(report + System.lineSeparator() + e.getMessage());
        }
        return report;
    }

    /**
     * Удаляет папку или файл
     * @param fileDir элемент файловой системы для которого работает метод
     * @return отчет об удалении
     */
    public String fileDirDelete(FileDir fileDir) {
        String report;
        try {
            filer.deleteFileDir(fileDir.getFdJavaIoFile());
            report = "Папка или файл: " + fileDir.getFdPath() + " удалена";
        } catch (Exception e) {
            report = "Не удалось удалить папку или файл: " + fileDir.getFdPath() + e.getMessage();
            log.error(report);
        }
        return report;
    }

    //endregion

    /**
     * Запускает команду(ы) в терминале
     * @param script команда(ы)
     * @return отчет о выполнении
     */
    protected String terminalExecute(String script) {
        String report;
        try {
            report = terminaler.processBuilderExecuteWithAnswer(script);
            log.info(report);
        } catch (Exception e) {
            report = "Не удалось выполнить команду(ы) в терминале: " + script;
            log.error(report + System.lineSeparator() + e.getMessage());
        }
        return report;
    }

    /**
     * Отложенный запуск копии бота и завершение текущей
     * @return отчет о выполнении
     */
    protected String botReset() {
        String report;
        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String currentJar = new File(new File("."), "Dogobot-0.0.1-SNAPSHOT.jar").getAbsolutePath();
        try {
            String sleep = "sleep 5";
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                sleep = "timeout /t 5 > nul";
            }
            terminaler.processBuilderExecute(sleep + " && " + javaBin + " -jar " + currentJar);
            report = "Отложенный запуск копии бота запущен. Следом запись вот этих логов в файл, завершение программы. Иначе - перехват исключений.";
            log.info(report);
            System.exit(0);
        } catch (Exception e) {
            report = "Не удалось отложенно запустить копию бота.";
            log.error(report + " Подробности:" + System.lineSeparator() + e.getMessage());
        }
        return report;
    }

}

