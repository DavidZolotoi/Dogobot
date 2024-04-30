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
     *
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
     *
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
     *
     * @param fileDir элемент файловой системы, для которого работает метод
     * @return идентификатор = случайная неповторимая строка на основе текущей даты и части строкового окончания.
     * Длина строки 30 символов, что вписывается в различные требования.
     */
    private String getPropertyFdId(FileDir fileDir) {
        if (fileDir.getFdId() != null) return fileDir.getFdId();
        final String txtForFinish = "%s%s".formatted(
                fileDir.getFdJavaIoFile().getAbsolutePath(), fileDir.getFdJavaIoFile().length()
        );
        String id = Screenshoter.getRandomStringDate(txtForFinish); //todo это и не только в блок try/catch
        return id;
    }

    /**
     * Получает (определяет) тип для FileDir.
     *
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
     *
     * @param fileDir элемент файловой системы, для которого работает метод
     * @return оригинальное название
     */
    private String getPropertyFdNameOriginal(FileDir fileDir) {
        return fileDir.getFdJavaIoFile().getName();
    }

    /**
     * Проверяет и в случае необходимости корректирует оригинальное название для соответствия требованиям к InlineKeyboardButton.
     * Корректировка происходит путём удаления неразрешенных символов и уменьшения длины строки до максимально допустимой.
     *
     * @param fileDir элемент файловой системы, для которого работает метод
     * @return проверенное и корректное название для InlineKeyboardButton
     */
    protected String getPropertyFdNameInlineButton(FileDir fileDir) {
        //Для поддержки других языков надо либо добавлять сюда,
        //либо менять на стратегию, чтоб в рег. выражении указать только запрещенные символы
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
     *
     * @param fileDir элемент файловой системы, для которого работает метод
     * @return CallbackData команда = FileDir.getFdId()
     */
    private String getPropertyFdCallbackData(FileDir fileDir) {
        return fileDir.getFdId();
    }

    /**
     * Получает полный путь к FileDir.
     *
     * @param fileDir элемент файловой системы, для которого работает метод
     * @return полный путь
     */
    private String getPropertyFdAbsolutePath(FileDir fileDir) {
        return fileDir.getFdJavaIoFile().getAbsolutePath();
    }

    /**
     * Получает дату последнего изменения FileDir.
     *
     * @param fileDir элемент файловой системы, для которого работает метод
     * @return дата последнего изменения
     */
    private long getPropertyFdDate(FileDir fileDir) {
        return fileDir.getFdJavaIoFile().lastModified();
    }

    /**
     * Получает занимаемый размер FileDir.
     *
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
     *
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
     *
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

    public String findUserOrRegister(Update update) {
        String smileBlush = EmojiParser.parseToUnicode(":blush:");
        String report = null;

        if (findUser(update) != null) {
            report = "Данные о пользователе найдены. Вы можете проверить их корректность вызвав соответствующую команду из меню" + smileBlush;
        } else {
            createAndRegisterUser(update);
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

    public User createAndRegisterUser(Update update) {
        String smileBlush = EmojiParser.parseToUnicode(":blush:");
        String report = null;
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
            //todo в теории везде, где ошибка, надо отчитаться пользователю, но не факт что прямо в этом методе
        }

        return user;
    }

    public User deleteUser(Update update) {
        String report = null;
        User user = findUser(update);
        if (user != null) {
            try {
                user = userer.deleteUser(user);
                report = "Данные о пользователе удалены." + System.lineSeparator() + user.toString();
                log.info(report);
            } catch (Exception e) {
                report = "Не удалось удалить данные о пользователе.";
                log.error(report);
            }
        } else {
            report = "Данных о пользователе не найдено.";
            log.error(report);
        }

        return user;
    }

    /**
     * Обновляет пароль упаковки/распаковки
     *
     * @param update          объект обновления
     * @param newPackPassword новый пароль
     * @return пользователь с обновленным паролем
     */
    public User updatePackPassword(Update update, String newPackPassword) {
        User user = findUser(update);
        if (user != null) {
            try {
                user = userer.updatePackPassword(user, newPackPassword);
                log.info("Попытка обновления пароля упаковки/распаковки для пользователя прошла без исключений. " + System.lineSeparator() + user.toString());
            } catch (Exception e) {
                log.error("При попытке обновления пароля упаковки/распаковки для пользователя возникло исключение. " + System.lineSeparator() + e.getMessage());
            }
        } else {
            log.error("Данных о пользователе не найдено.");
        }

        return user;
    }

    /**
     * Обновляет персональный адрес электронной почты
     *
     * @param update          объект обновления
     * @param newPersonalMail новый персональный адрес
     * @return пользователь с обновленным персональным адресом
     */
    public User updatePersonalMail(Update update, String newPersonalMail) {
        User user = findUser(update);
        if (user != null) {
            try {
                user = userer.updatePersonalEmail(user, newPersonalMail);
                log.info("Попытка обновления персонального адреса электронной почты для пользователя прошла без исключений. " + System.lineSeparator() + user.toString());
            } catch (Exception e) {
                log.error("При попытке обновления персонального адреса электронной почты для пользователя возникло исключение. " + System.lineSeparator() + e.getMessage());
            }
        } else {
            log.error("Данных о пользователе не найдено.");
        }

        return user;
    }

    /**
     * Обновляет другой адрес электронной почты
     *
     * @param update       объект обновления
     * @param newOtherMail новый другой адрес
     * @return пользователь с обновленным другой адрес
     */
    public User updateOtherMail(Update update, String newOtherMail) {
        User user = findUser(update);
        if (user != null) {
            try {
                user = userer.updateOtherEmail(user, newOtherMail);
                log.info("Попытка обновления другого адреса электронной почты для пользователя прошла без исключений. " + System.lineSeparator() + user.toString());
            } catch (Exception e) {
                log.error("При попытке обновления другого адреса электронной почты для пользователя возникло исключение. " + System.lineSeparator() + e.getMessage());
            }
        } else {
            log.error("Данных о пользователе не найдено.");
        }

        return user;
    }

    //endregion

    /**
     * Делает скриншот
     * @return путь к скриншоту
     */
    protected String printScreen(){
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
     *
     * @param fileDir   элемент файловой системы, для отправки
     * @param recipient адрес получателя
     * @return отчёт отправки
     */
    protected String sendEmailWithAttachment(FileDir fileDir, String recipient) {
        String report = null;
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

    //endregion

    //region РАБОТА С АРХИВАМИ

    /**
     * Упаковывает файл или папку (метод без пароля)
     *
     * @param sourceFileDir элемент файловой системы, для которого работает метод
     * @return отчет об упаковке
     */
    public String zipFileDirWithoutPassword(FileDir sourceFileDir) {
        String report = null;
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
     *
     * @param sourceFileDir элемент файловой системы, для которого работает метод
     * @param password      пароль для упаковки
     * @return отчет об упаковке
     */
    public String zipFileDirWithPassword(FileDir sourceFileDir, String password) {
        String report = null;
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
     * Распаковывает файл или папку (метод без пароля).
     * Если в родительской папке архива есть файлы или папки с именами, как в архиве, то они будут заменены.
     *
     * @param sourceFileDir элемент файловой системы, для которого работает метод.
     * @return отчет о распаковке
     */
    public String unzipFileDirWithoutPassword(FileDir sourceFileDir) {
        String report = null;
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
     *
     * @param sourceFileDir элемент файловой системы, для которого работает метод
     * @param password      пароль для распаковки
     * @return отчет о распаковке
     */
    public String unzipFileDirWithPassword(FileDir sourceFileDir, String password) {
        String report = null;
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

    //endregion

    //region РАБОТА С ФАЙЛАМИ

    /**
     * Переименовывает папку или файл
     * @param oldFileDir элемент файловой системы для которого работает метод
     * @param newName новое имя
     * @return отчет о переименовании
     */
    public String fileDirRename(FileDir oldFileDir, String newName) {
        String report = null;
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
        String report = null;
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
        String report = null;

        try {
            File newFile = filer.copyFileDir(
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
        String report = null;
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

