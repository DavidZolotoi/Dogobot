package ru.dogobot.Dogobot.service;

import com.vdurmont.emoji.EmojiParser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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
import ru.dogobot.Dogobot.model.FileDir;
import ru.dogobot.Dogobot.model.User;
import ru.dogobot.Dogobot.model.UserRepository;

import java.io.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    //todo посмотреть модификаторы доступа

    //region КОНСТАНТЫ и другие исходные данные
    final BotConfig botConfig;

    //команды меню
    @Getter  @AllArgsConstructor
    public enum BotMenuEnum {
        START(      "/start",       "Здравствуйте!"),
        SHOWDIR(    "/showdir",     "Показать содержимое папки"),   //todo переименовать. Варианты: "поехали","действия","home"...
        SCREENSHOT( "/screenshot",  "Сделать скриншот"),
        MYDATA(     "/mydata",      "Посмотреть данные о себе"),
        DELETEDATA( "/deletedata",  "Удалить данные о себе"),
        HELP(       "/help",        "Помощь"),
        SETTINGS(   "/settings",    "Настройки"),
        EXIT(       "/botstop",     "Остановить бота");

        private final String key;
        private final String description;
    }
    final List<BotCommand> botMenu;

    //нижняя клавиатура по умолчанию (только наименования в таблице)
    List<List<String>> replyKeyboardNames = new ArrayList<>(Arrays.asList(
            Arrays.asList("Домой", "Очистить чат")
            //Arrays.asList("button3", "button4")
    ));

    //endregion

    //region AUTOWIRED and CONSTRUCTORS

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileDir fileDir;

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
        //if (!ownerFilter(update)) return;
        if (update.hasMessage() && update.getMessage().hasText()) {             //Если прилетел текст
            handlerText(update);
        } else if (update.hasCallbackQuery()) {                                 //Если прилетел CallbackQuery
            handlerCallBackQuery(update);
        //todo можно добавить сохранение присланных файлов, но придется для каждого типа файла (музка, видео и т.п.) делать отдельный метод
        } else {
            log.error("Не могу распознать отправленную информацию: " + update);
        }
    }

    /**
     * Фильтр для блокировки бота для всех, кроме владельца
     * @param update объект обновления
     * @return true - обратился владелец
     */
    private boolean ownerFilter(Update update) {
        //todo переделать фильтр - не работает
        return Objects.equals(
                update.getMessage().getChatId(),
                this.botConfig.getOwnerId()
        );
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

        } else if (messageText.equals(BotMenuEnum.SHOWDIR.getKey())) {
            commandShowdirHandler(update);

        } else if (messageText.equals(BotMenuEnum.SCREENSHOT.getKey())) {
            commandScreenshotHandler(update);

        } else if (messageText.equals(BotMenuEnum.MYDATA.getKey())) {
            commandMydataHandler(update);

        } else if (messageText.equals(BotMenuEnum.DELETEDATA.getKey())) {
            commandDeletedataHandler(update);

        } else if (messageText.equals(BotMenuEnum.EXIT.getKey())) {
            commandExitHandler(update);

        } else {
            commandOrElseHandler(update);
        }
    }

    /**
     * Обработчик CallbackQuery
     * @param update объект обновления
     */
    private void handlerCallBackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        long messageId = update.getCallbackQuery().getMessage().getMessageId();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (fileManager.getCurrentPathDict().containsKey(callbackData)){
            FileDir targetFileDir = fileManager.getCurrentPathDict().get(callbackData);

            if (targetFileDir.getFdNameInline().equals(fileManager.MENU)){
                sendMessageWithoutKeyboard(chatId, "МЕНЮ");
                return;
            }

            targetFileDir = fileManager.getFileDirWithScan(targetFileDir.getFdPath());
            editMessageWithInlineKeyboard(
                    chatId,
                    messageId,
                    "%s: %s".formatted("Текущий путь ", targetFileDir.getFdPath())
                    , targetFileDir
            );
        }
        else {
            log.error("Не могу распознать нажатую кнопку: " + callbackData);
        }


    }

    //endregion

    //region Обработчики команд

    /**
     * Обработчик команды /start
     * @param update объект обновления
     */
    private void commandStartHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        String name = update.getMessage().getChat().getFirstName();
        String smileBlush = EmojiParser.parseToUnicode(":blush:");
        String answer = "Привет, %s, поздравляю с регистрацией в сервисе!%s".formatted(name, smileBlush);
        registerUser(update);
        sendMessageWithReplyKeyboard(chatId, answer);
        log.info("Пользователь %s. Команда START. Выполнено.".formatted(name));
    }
    /**
     * Метод для регистрации пользователя
     * @param update объект обновления
     */
    private void registerUser(Update update) {
        var message = update.getMessage();
        if(userRepository.findById(message.getChatId()).isEmpty()){
            User user = new User();
            user.setChatId(message.getChatId());
            user.setFirstName(message.getChat().getFirstName());
            user.setLastName(message.getChat().getLastName());
            user.setUserName(message.getChat().getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("В БД сохранен новый пользователь: " + user);
        }
        else {
            log.info("Пользователь %s уже зарегистрирован".formatted(message.getChatId()));
        }
    }

    /**
     * Обработчик команды /showdir
     * @param update объект обновления
     */
    private void commandShowdirHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        final String HOME_PATH = System.getProperty("user.home") + "/forTest";
        this.fileDir = fileManager.getFileDirWithScan(HOME_PATH);
        sendMessageWithInlineKeyboard(
                chatId,
                "%s: %s".formatted("Текущий путь ", this.fileDir.getFdPath()),
                this.fileDir
        );
    }

    private void commandScreenshotHandler(Update update) {
        //сделать скриншот экрана
        String screenPath = screenshoter.take();
        sendFile(update.getMessage().getChatId(), screenPath);
    }

    private void commandMydataHandler(Update update) {
        //todo доделать этот метод
        long chatId = update.getMessage().getChatId();
        sendMessageWithoutKeyboard(chatId, "Запрошены данные о себе");
    }

    private void commandDeletedataHandler(Update update) {
        //todo доделать этот метод
        long chatId = update.getMessage().getChatId();
        sendMessageWithoutKeyboard(chatId, "Запрошено удаление данных о себе");
    }

    private void commandExitHandler(Update update) {
        //todo доделать этот метод
        long chatId = update.getMessage().getChatId();
        sendMessageWithoutKeyboard(chatId, "Всё, выключаю бота на том устройстве");
        System.exit(0);
    }

    private void commandOrElseHandler(Update update) {
        //todo доделать этот метод
        long chatId = update.getMessage().getChatId();
        sendMessageWithoutKeyboard(chatId, "Прислано " + update.getMessage().getText());
    }

    //endregion

    //region ОТПРАВКА И ИЗМЕНЕНИЕ СООБЩЕНИЙ

    /**
     * Метод для внесения изменений (отправка или правка) в чат с подготовленным сообщением
     * @param message подготовленное сообщение
     */
    private void executeMessage(SendMessage message){
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Не удалось внести изменения в чат (отправить/изменить): " + e.getMessage());
        }
    }

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
     * Метод для отправки текстового сообщения с вызовом Inline-клавиатуры
     * @param chatId Id чата получателя
     * @param textMessage текстовое сообщение
     * @param fileDir элемент файловой системы, содержащий информацию для создания клавиатуры (названия кнопок, команды и т.п.)
     */
    private void sendMessageWithInlineKeyboard(long chatId, String textMessage, FileDir fileDir) {
        SendMessage message = new SendMessage();                            //создали сообщение
        message.setChatId(String.valueOf(chatId));                          //указали чат
        message.setText(textMessage);                                       //указали текст сообщения
        InlineKeyboardMarkup markupInLine = getInlineKeyboardMarkup(fileDir);
        message.setReplyMarkup(markupInLine);                               //добавили клавиатуру к сообщению

        executeMessage(message);                                            //внесли изменения в чат (отправили)
    }

    //4. ОТПРАВКА ФАЙЛА
    /**
     * Метод для отправки файла
     * @param chatId Id чата получателя
     * @param filePath путь к отправляемому файлу
     */
    private void sendFile(long chatId, String filePath){
        try {
            execute(new SendDocument(String.valueOf(chatId), new InputFile(new File(filePath))));
        } catch (TelegramApiException | NullPointerException e) {
            log.error("Не удалось отправить файл: " + filePath + System.lineSeparator() + e.getMessage());
        }
    }

    //5. ИЗМЕНЕНИЕ УЖЕ ОТПРАВЛЕННОГО СООБЩЕНИЯ
    /**
     * Метод для изменения уже отправленного сообщения
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
        } catch (TelegramApiException e) {
            log.error("ERROR_TEXT" + e.getMessage());
        }
    }

    //5. ИЗМЕНЕНИЕ УЖЕ ОТПРАВЛЕННОГО СООБЩЕНИЯ С IНЛАЙН-КЛАВИАТУРОЙ
    /**
     * Метод для изменения уже отправленного сообщения
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
        } catch (TelegramApiException e) {
            log.error("ERROR_TEXT" + e.getMessage());
        }
    }
    //endregion

    //region КЛАВИАТУРЫ

    //СОЗДАНИЕ И ВОЗВРАТ Inline-КЛАВИАТУРЫ
    /**
     * Метод создания и возврата Inline-клавиатуры на основе переданной таблицы названий кнопок.
     * Из недостатков - единое правило на все команды CallbackData.
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
