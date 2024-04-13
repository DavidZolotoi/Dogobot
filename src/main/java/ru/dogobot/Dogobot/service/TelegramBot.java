package ru.dogobot.Dogobot.service;

import com.vdurmont.emoji.EmojiParser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.dogobot.Dogobot.config.BotConfig;
import ru.dogobot.Dogobot.config.JsonData;
import ru.dogobot.Dogobot.model.FileDir;
import ru.dogobot.Dogobot.model.User;
import ru.dogobot.Dogobot.model.UserRepository;

import java.io.*;
import java.sql.Timestamp;
import java.util.*;

@Slf4j
@Service
public class TelegramBot extends TelegramLongPollingBot {
    //todo посмотреть модификаторы доступа

    //region КОНСТАНТЫ и другие исходные данные
    final BotConfig botConfig;

    //команды меню
    @Getter  @AllArgsConstructor
    public enum BotMenuEnum {
        START(      "/start",       "Здравствуйте!"),
        SHOWHOME(   "/showhome",    "Показать домашнюю папку"),
        GOTO(       "/goto",        "Перейти по адресу"),
        SCREENSHOT( "/screenshot",  "Сделать скриншот"),
        MYDATA(     "/mydata",      "Посмотреть данные о себе"),
        DELETEDATA( "/deletedata",  "Удалить данные о себе"),
        HELP(       "/help",        "Помощь"),
        SETTINGS(   "/settings",    "Настройки"),
        RESET(      "/botreset",    "Перезапустить бота"),
        EXIT(       "/botstop",     "Остановить бота");

        private final String key;
        private final String description;
    }
    final List<BotCommand> botMenu;

    //нижняя клавиатура по умолчанию (только наименования в таблице)
    List<List<String>> replyKeyboardNames = new ArrayList<>(
            List.of( Arrays.asList("Домой", "Очистить чат") )
    );

    private long fileDirMenuMessageId;

    //endregion

    //region AUTOWIRED and CONSTRUCTORS

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileManager fileManager;

    @Autowired
    private Screenshoter screenshoter;

    public TelegramBot(BotConfig botConfig) {
        this.botConfig = botConfig;
        this.botMenu = getBotMenu();
    }
    /**
     * Заполняет меню бота командами, считывая их из BotMenuEnum
     * @return коллекцию команд
     */
    private List<BotCommand> getBotMenu() {
        List<BotCommand> botMenu = new ArrayList<>();
        for (var botCommand : BotMenuEnum.values()) {
            botMenu.add(new BotCommand(botCommand.getKey(), botCommand.getDescription()));
        }
        try {
            this.execute(new SetMyCommands(botMenu, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Не удалось добавить команды: " + e.getMessage());
        }
        return botMenu;
    }

    //endregion

    @Override
    public String getBotUsername() {
        return this.botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return this.botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!ownerFilter(update)) return;
        if (update.hasMessage() && update.getMessage().hasText()) {             //Если прилетел текст
            handlerText(update);
        } else if (update.hasCallbackQuery()) {                                 //Если прилетел CallbackQuery
            handlerCallBackQuery(update);
        //todo можно добавить сохранение присланных файлов, но придется для каждого типа файла (музыка, видео и т.п.) делать отдельный метод
        } else {
            log.error("Не могу распознать отправленную информацию: " + update);
        }
    }

    /**
     * Фильтр для идентификации владельца
     * @param update объект обновления
     * @return true - обратился владелец
     */
    private boolean ownerFilter(Update update) {
        Long ownerId = botConfig.getOwnerId();
        return ( update.getMessage() != null && update.getMessage().getFrom().getId().equals(ownerId) ) ||
                ( update.getCallbackQuery() != null && update.getCallbackQuery().getFrom().getId().equals(ownerId) );
    }

    //region Обработчики входной информации

    /**
     * Обработчик текстовых сообщений
     * @param update объект обновления
     */
    private void handlerText(Update update) {
        String messageText = update.getMessage().getText();

        if (messageText.equals(BotMenuEnum.START.getKey())) {
            commandStartHandler(update);

        } else if (messageText.equals(BotMenuEnum.SHOWHOME.getKey())) {
            commandShowhomeHandler(update);

        } else if (messageText.equals(BotMenuEnum.GOTO.getKey())) {
            commandGotoHandler(update);

        } else if (messageText.equals(BotMenuEnum.SCREENSHOT.getKey())) {
            commandScreenshotHandler(update);

        } else if (messageText.equals(BotMenuEnum.MYDATA.getKey())) {
            commandMydataHandler(update);

        } else if (messageText.equals(BotMenuEnum.DELETEDATA.getKey())) {
            commandDeletedataHandler(update);

        } else if (messageText.equals(BotMenuEnum.HELP.getKey())) {
            commandHelpHandler(update);

        } else if (messageText.equals(BotMenuEnum.SETTINGS.getKey())) {
            commandSettingsHandler(update);

        } else if (messageText.equals(BotMenuEnum.RESET.getKey())) {
            commandResetHandler(update);

        } else if (messageText.equals(BotMenuEnum.EXIT.getKey())) {
            commandExitHandler(update);

        } else {
            //варианты с дробью, но с аргументами
            if ( update.getMessage().getText().substring(0, 6).equals("/goto ") ) {
                commandGotoArgsHandler(update);

            } else if ( update.getMessage().getText().substring(0, 10).equals("/set_pass ") ){
                commandSetPassHandler(update);

            } else if ( update.getMessage().getText().substring(0, 10).equals("/set_mail ") ){
                commandSetMailHandler(update);

            } else if ( update.getMessage().getText().substring(0, 4).equals("/rn ") ) {
                commandRenameHandler(update);

            } else if ( update.getMessage().getText().substring(0, 4).equals("/mv ") ) {
                commandMoveHandler(update);

            } else if ( update.getMessage().getText().substring(0, 4).equals("/cp ") ) {
                commandCopyHandler(update);

            } else if ( update.getMessage().getText().substring(0, 5).equals("/cmd ") ) {
                commandCMDHandler(update);

            } else {
                commandOrElseHandler(update);
            }
        }
    }

    /**
     * Обработчик команд Callback
     * @param update объект обновления
     */
    private void handlerCallBackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        long messageId = update.getCallbackQuery().getMessage().getMessageId();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        System.out.println(callbackData);

        // если callbackData - один из пунктов словаря актуальной папки
        if (fileManager.getCurrentPathDict().containsKey(callbackData)){
            FileDir targetFileDir = fileManager.getCurrentPathDict().get(callbackData);

            //если нажали на кнопку МЕНЮ
            if (targetFileDir.getFdNameInline().equals(fileManager.MENU)){
                sendMessageWithInlineFileDirMenu(
                        chatId,
                        FileManager.SELECT_MENU_ITEM,
                        FileManager.FileDirMenu.values()
                );
                return;
            }

            //если нажали на кнопку на элемент файловой системы
            targetFileDir = fileManager.getFileDirWithScan(targetFileDir.getFdPath());
            editMessageWithInlineKeyboard(
                    chatId,
                    messageId,
                    "%s: %s".formatted("Текущий путь ", targetFileDir.getFdPath()),
                    targetFileDir
            );
            return;
        }

        //если нажали на кнопку "Получить информацию"
        if (callbackData.equals(FileManager.FileDirMenu.GET_INFO.getButtonCallback())){
            sendMessageWithoutKeyboard(
                    chatId,
                    fileManager.getFileDir().toString()
            );
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали на кнопку "Получить в телеграм"
        if (callbackData.equals(FileManager.FileDirMenu.GET_ON_TELEGRAM.getButtonCallback())){
            if (fileManager.getFileDir().getFdType().equals(FileDir.FDType.FILE)) {
                sendFile(chatId, fileManager.getFileDir().getFdPath());
            } else {
                sendMessageWithoutKeyboard(
                        chatId,
                        "Это папка, для отправки в телеграм, её сначала нужно упаковать в архив."
                );
            }
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали на кнопку "Получить на почту"
        if (callbackData.equals(FileManager.FileDirMenu.GET_ON_EMAIL.getButtonCallback())){
            if (fileManager.getFileDir().getFdType().equals(FileDir.FDType.FILE)) {
                fileManager.sendEmailWithAttachment(fileManager.getFileDir());
                sendMessageWithoutKeyboard(
                        chatId,
                        "'%s' отправлен на почту".formatted(fileManager.getFileDir().getFdPath())
                );
            } else {
                sendMessageWithoutKeyboard(
                        chatId,
                        "Это папка, для отправки на почту, её сначала нужно упаковать в архив."
                );
            }
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали на кнопку "Отправить на почту"
        if (callbackData.equals(FileManager.FileDirMenu.SEND_TO_EMAIL.getButtonCallback())){
            if (fileManager.getFileDir().getFdType().equals(FileDir.FDType.FILE)) {
                fileManager.sendEmailWithAttachment(fileManager.getFileDir(), "dogobot@bk.ru"); //todo достать из настроек пользователя
                sendMessageWithoutKeyboard(
                        chatId,
                        "'%s' отправлен на почту из настроек".formatted(fileManager.getFileDir().getFdPath())
                );
            } else {
                sendMessageWithoutKeyboard(
                        chatId,
                        "Это папка, для отправки на почту, её сначала нужно упаковать в архив."
                );
            }
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали на кнопку "Упаковать в zip"
        if (callbackData.equals(FileManager.FileDirMenu.PACK.getButtonCallback())){
            fileManager.zipFileDirWithoutPassword(
                    fileManager.getFileDir()
            );
            sendMessageWithoutKeyboard(chatId, "Папка успешно сжата в архив без установки пароля.");
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали на кнопку "Упаковать в zip с паролем"
        if (callbackData.equals(FileManager.FileDirMenu.PACK_WITH_PASSWORD.getButtonCallback())){
            fileManager.zipFileDirWithPassword(
                    fileManager.getFileDir(),
                    "111"   //todo достать из настроек пользователя
            );
            sendMessageWithoutKeyboard(chatId, "Папка успешно сжата в архив с установкой пароля.");
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали "Распаковать из zip"
        if (callbackData.equals(FileManager.FileDirMenu.UNPACK.getButtonCallback())){
            fileManager.unzipFileDirWithoutPassword(
                    fileManager.getFileDir()
            );
            sendMessageWithoutKeyboard(chatId, "Архив успешно распакован без пароля.");
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали "Распаковать из zip с паролем"
        if (callbackData.equals(FileManager.FileDirMenu.UNPACK_WITH_PASSWORD.getButtonCallback())){
            fileManager.unzipFileDirWithPassword(
                    fileManager.getFileDir(),
                    "111"   //todo достать из настроек пользователя
            );
            sendMessageWithoutKeyboard(chatId, "Архив успешно распакован с паролем.");
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали на кнопку "Переименовать"
        if (callbackData.equals(FileManager.FileDirMenu.RENAME.getButtonCallback())){
            sendMessageWithoutKeyboard(chatId, "Для переименования текущего файла/папки введите команду (без кавычек и фигурных скобок) в формате '/rn {новое имя}'");
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали кнопку "Переместить"
        if (callbackData.equals(FileManager.FileDirMenu.MOVE.getButtonCallback())){
            sendMessageWithoutKeyboard(chatId, "Для перемещения текущего файла/папки введите команду (без кавычек и фигурных скобок) в формате '/mv {новый путь к папке для перемещения}'");
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали кнопку "Копировать"
        if (callbackData.equals(FileManager.FileDirMenu.COPY.getButtonCallback())){
            sendMessageWithoutKeyboard(chatId, "Для копирования текущего файла/папки введите команду (без кавычек и фигурных скобок) в формате '/cp {новый путь к папке для копирования}'");
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали на кнопку "Удалить"
        if (callbackData.equals(FileManager.FileDirMenu.DELETE.getButtonCallback())){
            String report = fileManager.fileDirDelete(
                    fileManager.getFileDir()
            );
            sendMessageWithoutKeyboard(chatId, report);
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали на кнопку "Убрать меню"
        if (callbackData.equals(FileManager.FileDirMenu.REMOVE_MENU.getButtonCallback())){
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        log.error("Не могу распознать нажатую кнопку: " + callbackData);
    }

    //endregion

    //region Обработчики команд

    /**
     * Обработчик команды /start
     * @param update объект обновления
     */
    private void commandStartHandler(Update update) {
        User user = registerOrGetUser(update);

        String smileBlush = EmojiParser.parseToUnicode(":blush:");
        String report = "Привет, %s, поздравляю с регистрацией в сервисе!%s"
                .formatted(user.getFirstName(), smileBlush);
        sendMessageWithReplyKeyboard(user.getChatId(), report);
        log.info("Пользователь %s. Команда START. Выполнено.".formatted(user.getFirstName()));
    }
    /**
     * Проверяет, зарегистрирован ли пользователь, если нет, то регистрирует
     * @param update объект обновления
     * @return зарегистрированного пользователя
     */
    private User registerOrGetUser(Update update) {
        User user = new User();
        var message = update.getMessage();
        if(userRepository.findById(message.getChatId()).isEmpty()){
            user.setChatId(message.getFrom().getId());
            user.setFirstName(message.getChat().getFirstName());
            user.setLastName(message.getChat().getLastName());
            user.setUserName(message.getChat().getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            user.setPackPassword("1111");//user.getUserDefaultConfig().getPackPassword());

            userRepository.save(user);
            log.info("В БД сохранен новый пользователь: " + user);
        }
        else {
            user = userRepository.findById(message.getChatId()).get();
            log.info("Пользователь %s уже зарегистрирован".formatted(message.getChatId()));
        }
        return user;
    }

    /**
     * Обработчик команды /showdir
     * @param update объект обновления
     */
    private void commandShowhomeHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        final String HOME_PATH = System.getProperty("user.home") + "/forTest";
        FileDir fileDir = fileManager.getFileDirWithScan(HOME_PATH);
        sendMessageWithInlineKeyboard(
                chatId,
                "%s: %s".formatted("Текущий путь ", fileDir.getFdPath()),
                fileDir
        );
    }

    /**
     * Обработчик команды /goto
     * @param update объект обновления
     */
    private void commandGotoHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        sendMessageWithoutKeyboard(
                chatId,
                "Для того, чтоб напрямую перейти по адресу, введите команду (без кавычек и фигурных скобок) в формате '/goto {путь для перехода}'"
        );
    }

    /**
     * Обработчик команды /goto с аргументами (путём для перехода)
     * @param update объект обновления
     */
    private void commandGotoArgsHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        String targetPath = update.getMessage().getText().substring("/goto ".length());
        FileDir fileDir = fileManager.getFileDirWithScan(targetPath);
        sendMessageWithInlineKeyboard(
                chatId,
                "%s: %s".formatted("Текущий путь ", fileDir.getFdPath()),
                fileDir
        );
    }

    /**
     * Обработчик команды /setpass
     * @param update объект обновления
     */
    private void commandSetPassHandler(Update update) {
        //todo доделать этот метод
    }

    /**
     * Обработчик команды /setmail
     * @param update объект обновления
     */
    private void commandSetMailHandler(Update update) {
        //todo доделать этот метод
    }

    /**
     * Обработчик команды /screenshot
     * @param update объект обновления
     */
    private void commandScreenshotHandler(Update update) {
        //сделать скриншот экрана
        String screenPath = screenshoter.take();
        sendFile(update.getMessage().getChatId(), screenPath);
    }

    /**
     * Обработчик команды /mydata
     * @param update объект обновления
     */
    private void commandMydataHandler(Update update) {
        String report = null;
        User user = new User();
        var message = update.getMessage();
        if(userRepository.findById(message.getChatId()).isEmpty()){
            report = "Пользователь не зарегистрирован, данных нет.";
        }
        else {
            user = userRepository.findById(message.getChatId()).get();
            report = user.toString();
        }
        sendMessageWithoutKeyboard(user.getChatId(), report);
    }

    /**
     * Обработчик команды /deletedata
     * @param update объект обновления
     */
    private void commandDeletedataHandler(Update update) {
        String report = null;
        User user = new User();
        var message = update.getMessage();
        if(userRepository.findById(message.getChatId()).isEmpty()){
            report = "Данных о пользователе не найдено.";
        }
        else {
            user = userRepository.findById(message.getChatId()).get();
            userRepository.delete(user);
            report = "Данные о пользователе удалены.";
        }
        //todo добавить полную очистку чата и оставить кнопку /start
        sendMessageWithoutKeyboard(user.getChatId(), report);
    }

    private void commandHelpHandler(Update update) {
        //todo доделать этот метод
    }

    private void commandSettingsHandler(Update update) {
        //todo
        // 1. Заменить литералы на значения из источников
        // 2. Создать команды для изменения пароля и другой почты /set_pass и /set_mail
        // 3. Ругаться если вызвали команду с исп. незаполненных данных
        // 4. Всю программу проверить на корректность работы с разными пользователями, добавить логи, выводы, обработку исключений, возможно тесты

        String sep = System.lineSeparator();
        StringBuilder report = new StringBuilder();
        report.append("Личные настройки:").append(sep);
        report.append("---").append(sep);
        report.append("Пароль (упаковка, распаковка и т.п.): ").append("111").append(sep);
        report.append("Для изменения пароля введите команду (без кавычек и фигурных скобок) в формате '/set_pass {новый пароль}'. Например: /set_pass 111").append(sep);
        report.append("---").append(sep);
        report.append("Личная почта (для получения на неё писем): ").append("111").append(sep);
        report.append("Личную почту вместе с остальными настройками (логин, пароль, вход./исходящ. серверы и порты) можно поменять только на устройстве, где запущен бот.").append(sep);
        report.append("---").append(sep);
        report.append("Другая почта (для отправки на неё писем): ").append("111").append(sep);
        report.append("Для изменения другой почты (для отправки на неё писем) введите команду (без кавычек и фигурных скобок) в формате '/set_mail {новый адрес другой почты}'. Например: /set_mail mynew@other.mail").append(sep);
        sendMessageWithoutKeyboard(update.getMessage().getChatId(), report.toString());
    }

    /**
     * Обработчик команды /reset
     * @param update объект обновления
     */
    private void commandResetHandler(Update update) {
        fileManager.terminaler.appCloneAndClose();
    }

    /**
     * Обработчик команды /exit
     * @param update объект обновления
     */
    private void commandExitHandler(Update update) {
        //todo доделать этот метод
        long chatId = update.getMessage().getChatId();
        sendMessageWithoutKeyboard(chatId, "Всё, выключаю бота на том устройстве");
        System.exit(0);
    }

    //варианты с дробью, но с аргументами
    private void commandRenameHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        String report = fileManager.fileDirRename(
                fileManager.getFileDir(),
                update.getMessage().getText().substring("/rn ".length())
        );
        sendMessageWithoutKeyboard(chatId, report);
    }

    private void commandMoveHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        String report = fileManager.fileDirMove(
                fileManager.getFileDir(),
                update.getMessage().getText().substring("/mv ".length())
        );
        sendMessageWithoutKeyboard(chatId, report);
    }

    private void commandCopyHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        String report = fileManager.fileDirCopy(
                fileManager.getFileDir(),
                update.getMessage().getText().substring("/cp ".length())
        );
        sendMessageWithoutKeyboard(chatId, report);
    }

    private void commandCMDHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        String report = fileManager.terminalExecute(
                update.getMessage().getText().substring("/cmd ".length())
        );// commandRNHandler
        sendMessageWithoutKeyboard(chatId, report);
    }

    /**
     * Обработчик команд не из списка
     * @param update объект обновления
     */
    private void commandOrElseHandler(Update update) {
        //todo доделать этот метод
        long chatId = update.getMessage().getChatId();
        sendMessageWithoutKeyboard(chatId, "Непонятно что делать с командой: " + update.getMessage().getText());
    }

    //endregion

    //region ОТПРАВКА И ИЗМЕНЕНИЕ СООБЩЕНИЙ

    //1. ПРОСТАЯ ОТПРАВКА СООБЩЕНИЯ
    /**
     * Метод для отправки текстового сообщения без вызова клавиатур
     * @param chatId Id чата получателя
     * @param textMessage текстовое сообщение
     */
    private void sendMessageWithoutKeyboard(long chatId, String textMessage){
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textMessage);
        executeMessage(message);
    }

    //2. ОТПРАВКА СООБЩЕНИЯ С КЛАВИАТУРОЙ
    /**
     * Метод для отправки текстового сообщения с вызовом Reply-клавиатуры
     * @param chatId Id чата получателя
     * @param textMessage текстовое сообщение
     */
    private void sendMessageWithReplyKeyboard(long chatId, String textMessage) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textMessage);

        ReplyKeyboardMarkup keyboardMarkup = getReplyKeyboardMarkup();
        message.setReplyMarkup(keyboardMarkup);

        executeMessage(message);
    }

    //3. ОТПРАВКА СООБЩЕНИЯ С ИНЛАЙН-КЛАВИАТУРОЙ
    /**
     * Метод для отправки текстового сообщения с вызовом Inline-клавиатуры (на основе элемента файловой системы)
     * @param chatId Id чата получателя
     * @param textMessage текстовое сообщение
     * @param fileDir элемент файловой системы, содержащий информацию для создания клавиатуры (названия кнопок, команды и т.п.)
     */
    private void sendMessageWithInlineKeyboard(long chatId, String textMessage, FileDir fileDir) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textMessage);
        InlineKeyboardMarkup markupInLine = getInlineKeyboardMarkup(fileDir);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }
    /**
     * Метод для отправки текстового сообщения с вызовом Inline-клавиатуры (на основе переданного массива из именованных констант)
     * @param chatId Id чата получателя
     * @param textMessage текстовое сообщение
     * @param fileDirMenuSortedCallbacks массив из именованных констант, содержащий как Callback-команды, так и названия кнопок
     *                                   (предоставляет класс файлового менеджера)
     */
    private void sendMessageWithInlineFileDirMenu(long chatId, String textMessage, FileManager.FileDirMenu[] fileDirMenuSortedCallbacks) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textMessage);
        InlineKeyboardMarkup markupInLine = getInlineKeyboardMarkup(fileDirMenuSortedCallbacks);
        message.setReplyMarkup(markupInLine);

        this.fileDirMenuMessageId = executeMessage(message);

    }

    /**
     * Метод для отправки подготовленного сообщением в чат
     * @param message подготовленное сообщение
     */
    private Integer executeMessage(SendMessage message){
        Integer messageId = null;
        try {
            messageId = execute(message).getMessageId();
            log.info("Сообщение с id " + messageId + " в чат " + message.getChatId() + " отправлено");
        } catch (TelegramApiException e) {
            log.error("Не удалось отправить сообщение в чат: " + e.getMessage());
        }
        return messageId;
    }

    //4. ОТПРАВКА ФАЙЛА
    /**
     * Метод для отправки файла в чат
     * @param chatId Id чата получателя
     * @param filePath путь к отправляемому файлу
     */
    private void sendFile(long chatId, String filePath){
        try {
            execute(new SendDocument(String.valueOf(chatId), new InputFile(new File(filePath))));
            log.info("Файл " + filePath + " отправлен в чате " + chatId);
        } catch (TelegramApiException | NullPointerException e) {
            log.error("Не удалось отправить файл: " + filePath + System.lineSeparator() + e.getMessage());
        }
    }

    //5. ИЗМЕНЕНИЕ УЖЕ ОТПРАВЛЕННОГО СООБЩЕНИЯ
    //  1ая перегрузка
    /**
     * Метод для изменения уже отправленного сообщения без добавления клавиатур
     * @param chatId Id чата получателя
     * @param messageId Id сообщения
     * @param textMessage текстовое сообщение для замены старого сообщения
     */
    private void editMessageWithoutKeyboard(long chatId, long messageId, String textMessage){
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(textMessage);
        message.setMessageId((int) messageId);

        try {
            execute(message);
            log.info("Сообщение с id " + messageId + " в чате " + chatId + " изменено");
        } catch (TelegramApiException e) {
            log.error("Не удалось изменить сообщение с id " + messageId + " в чате: " + e.getMessage());
        }
    }
    //  2ая перегрузка
    /**
     * Метод для изменения уже отправленного сообщения с добавлением Inline-клавиатуры
     * @param chatId Id чата получателя
     * @param messageId Id сообщения
     * @param textMessage текстовое сообщение для замены старого сообщения
     * @param fileDir элемент файловой системы, содержащий информацию для создания клавиатуры (названия кнопок, команды и т.п.)
     */
    private void editMessageWithInlineKeyboard(long chatId, long messageId, String textMessage, FileDir fileDir){
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(textMessage);
        message.setMessageId((int) messageId);

        InlineKeyboardMarkup markupInLine = getInlineKeyboardMarkup(fileDir);
        message.setReplyMarkup(markupInLine);

        try {
            execute(message);
            log.info("Сообщение с id " + messageId + " в чате " + chatId + " изменено");
        } catch (TelegramApiException e) {
            log.error("Не удалось изменить сообщение с id " + messageId + " в чате: " + e.getMessage());
        }
    }

    //6. УДАЛЕНИЕ УЖЕ ОТПРАВЛЕННОГО СООБЩЕНИЯ С IНЛАЙН-КЛАВИАТУРОЙ
    private void deleteMessageWithFileDirMenu(long chatIdLong){
        Integer messageId = (int) this.fileDirMenuMessageId;
        String chatId = String.valueOf(chatIdLong);
        DeleteMessage deleteMessage = new DeleteMessage(chatId, messageId);

        try {
            execute(deleteMessage);
            log.info("Сообщение с id " + messageId + " в чате " + chatId + " удалено");
        } catch (TelegramApiException e) {
            log.error("Не удалось удалить сообщение с id " + messageId + " в чате: " + e.getMessage());
        }
    }

    //endregion

    //region КЛАВИАТУРЫ

    //СОЗДАНИЕ И ВОЗВРАТ Inline-КЛАВИАТУРЫ.
    //      1ая перегрузка - для файловой системы
    /**
     * Метод создания и возврата Inline-клавиатуры на основе переданной fileDir.
     * ВАЖНО - единое правило на все команды CallbackData.
     * @param fileDir элемент файловой системы, содержащий информацию для создания клавиатуры (названия кнопок, команды и т.п.)
     * @return Inline-клавиатура
     */
    private InlineKeyboardMarkup getInlineKeyboardMarkup(FileDir fileDir) {
        InlineKeyboardMarkup iKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> iRows = new ArrayList<>();
        for (var inlineKeyboardIdsRow : fileDir.getFdInlineKeyboardIds()) {
            List<InlineKeyboardButton> iRow = new ArrayList<>();
            for(var inlineKeyboardId : inlineKeyboardIdsRow){
                InlineKeyboardButton iButton = new InlineKeyboardButton();
                FileDir fileDirInFolder = fileManager.getCurrentPathDict().get(inlineKeyboardId);
                iButton.setText(fileDirInFolder.getFdNameInline());
                iButton.setCallbackData(fileDirInFolder.getFdCallbackData());
                iRow.add(iButton);
            }
            iRows.add(iRow);
        }
        iKeyboard.setKeyboard(iRows);
        return iKeyboard;
    }
    //      2ая перегрузка - для меню элементов файловой системы
    /**
     * Метод создания и возврата Inline-клавиатуры на основе переданного массива из именованных констант.
     * @param fileDirMenuSortedCallbacks массив из именованных констант, содержащий как Callback-команды, так и названия кнопок
     *                                   (предоставляет класс файлового менеджера)
     * @return Inline-клавиатура
     */
    private InlineKeyboardMarkup getInlineKeyboardMarkup(FileManager.FileDirMenu[] fileDirMenuSortedCallbacks) {
        InlineKeyboardMarkup iKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> iRows = new ArrayList<>();
        for (int i = 0; i < fileDirMenuSortedCallbacks.length; i++) {
            List<InlineKeyboardButton> iRow = new ArrayList<>();
            InlineKeyboardButton iButton = new InlineKeyboardButton();
            iButton.setText(fileDirMenuSortedCallbacks[i].getButtonText());
            iButton.setCallbackData(fileDirMenuSortedCallbacks[i].getButtonCallback());
            iRow.add(iButton);
            iRows.add(iRow);
        }
        iKeyboard.setKeyboard(iRows);
        return iKeyboard;
    }

    /**
     * Метод создания и возврата Reply-клавиатуры по умолчанию
     * @return Reply-клавиатура
     */
    private ReplyKeyboardMarkup getReplyKeyboardMarkup() {
        return getReplyKeyboardMarkup(this.replyKeyboardNames);
    }
    /**
     * Метод создания и возврата Reply-клавиатуры
     * @param replyKeyboardNames названия кнопок
     * @return Reply-клавиатура
     */
    private ReplyKeyboardMarkup getReplyKeyboardMarkup(List<List<String>> replyKeyboardNames) {
        ReplyKeyboardMarkup rKeyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> rRows = new ArrayList<>();
        for (var replyKeyboardNamesRow : replyKeyboardNames) {
            KeyboardRow rRow = new KeyboardRow();
            for (var rName : replyKeyboardNamesRow) {
                rRow.add(rName);
            }
            rRows.add(rRow);
        }
        rKeyboard.setKeyboard(rRows);
        //rKeyboard.setOneTimeKeyboard(true);
        rKeyboard.setResizeKeyboard(true);
        return rKeyboard;
    }

    //endregion

}
