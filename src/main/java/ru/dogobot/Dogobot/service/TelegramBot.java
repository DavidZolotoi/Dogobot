package ru.dogobot.Dogobot.service;

import com.vdurmont.emoji.EmojiParser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.dogobot.Dogobot.config.BotConfig;
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
        SHOWDIR(    "/showdir",     "Показать содержимое папки"),
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

    @Autowired
    private UserRepository userRepository;

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
        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();

        if (messageText.equals(BotMenuEnum.START.getKey())) {
            commandStartHandler(update);

        } else if (messageText.equals(BotMenuEnum.SHOWDIR.getKey())) {
            commandShowdirHandler(update);

        } else if (messageText.equals(BotMenuEnum.MYDATA.getKey())) {
            sendMessageWithoutKeyboard(chatId, "Запрошены данные о себе");

        } else if (messageText.equals(BotMenuEnum.DELETEDATA.getKey())) {
            sendMessageWithoutKeyboard(chatId, "Запрошено удаление данных о себе");

        } else if (messageText.equals(BotMenuEnum.EXIT.getKey())) {
            sendMessageWithoutKeyboard(chatId, "Всё, выключаю бота на том устройстве");
            System.exit(0);

        } else {
            sendMessageWithoutKeyboard(chatId, "Прислано " + messageText);
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

        if(callbackData.equals("YesCD")){
            String text = "You pressed YES button";
            editMessageWithoutKeyboard(chatId, messageId, text);

            // получаем текущую директорию
            String line = null;
            try {
                Process process = Runtime.getRuntime().exec("pwd");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                line = reader.readLine();
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            sendMessageWithoutKeyboard(chatId, line);

            //todo начальные эксперименты с движением по файловой системе
            List<String> fileNames = new ArrayList<>();
            // Получаем домашнюю папку пользователя
            String homeDirectory = System.getProperty("user.home") + "/forTest";
            // Создаем объект File для представления домашней папки
            File folder = new File(homeDirectory);
            // Получаем список файлов в папке
            File[] files = folder.listFiles();
            // Получаем имена файлов на экран
            for(File file : files) {
                fileNames.add(file.getName());
            }
            sendMessageWithoutKeyboard(chatId, fileNames.toString());

            //сделать скриншот экрана
            String screenPath = screenshoter.take();
            sendFile(chatId, screenPath);

        }
        else if(callbackData.equals("NoCD")){
            String text = "You pressed NO button";
            editMessageWithoutKeyboard(chatId, messageId, text);
        }
        else {
            log.error("Не распознал callbackData: " + callbackData);
        }
    }

    //endregion

    /**
     * Обработчик команды /start
     * @param update объект обновления
     */
    private void commandStartHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        String name = update.getMessage().getChat().getFirstName();
        String answer = EmojiParser.parseToUnicode("Hi, " + name + ", nice to meet you!" + " :blush:");
        registerUser(update.getMessage());
        log.info("Replied to user " + name);
        sendMessageWithReplyKeyboard(chatId, answer);
    }
    /**
     * Метод для регистрации пользователя
     * @param message сообщение
     */
    private void registerUser(Message message) {
        if(userRepository.findById(message.getChatId()).isEmpty()){
            User user = new User();
            user.setChatId(message.getChatId());
            user.setFirstName(message.getChat().getFirstName());
            user.setLastName(message.getChat().getLastName());
            user.setUserName(message.getChat().getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("user saved: " + user);
        }
    }

    /**
     * Обработчик команды /showdir
     * @param update объект обновления
     */
    private void commandShowdirHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        List<List<String>> inlineKeyboardNames = new ArrayList<>(Arrays.asList(
                Arrays.asList("Yes", "No")
        ));
        sendMessageWithInlineKeyboard(chatId, "Do you really want to show directory content?", inlineKeyboardNames);
    }

    /**
     * Метод для внесения изменений в чат с подготовленным сообщением (отправка или правка)
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
    private void sendMessageWithoutKeyboard(long chatId, String textMessage){
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textMessage);
        executeMessage(message);
    }

    //2. ОТПРАВКА СООБЩЕНИЯ С КЛАВИАТУРОЙ
    private void sendMessageWithReplyKeyboard(long chatId, String textMessage) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textMessage);

        ReplyKeyboardMarkup keyboardMarkup = getReplyKeyboardMarkup();
        message.setReplyMarkup(keyboardMarkup);

        executeMessage(message);
    }

    //3. ОТПРАВКА СООБЩЕНИЯ С ИНЛАЙН-КЛАВИАТУРОЙ
    private void sendMessageWithInlineKeyboard(long chatId, String textMessage, List<List<String>> inlineKeyboardNames) {
        SendMessage message = new SendMessage();                            //создали сообщение
        message.setChatId(String.valueOf(chatId));                          //указали чат
        message.setText(textMessage);                                       //указали текст сообщения
        InlineKeyboardMarkup markupInLine = getInlineKeyboardMarkup(inlineKeyboardNames);
        message.setReplyMarkup(markupInLine);                               //добавили клавиатуру к сообщению

        executeMessage(message);                                            //внесли изменения в чат (отправили)
    }

    //4. ОТПРАВКА ФАЙЛА
    /**
     * Метод для отправки файла
     * @param chatId Id чата
     * @param filePath путь к файлу
     */
    private void sendFile(long chatId, String filePath){
        //filePath = "/home/delllindeb/archive/myproject/Dogobot/Other/Dogobot.jpg";
        try {
            execute(new SendDocument(String.valueOf(chatId), new InputFile(new File(filePath))));
        } catch (TelegramApiException | NullPointerException e) {
            log.error("Не удалось отправить файл: " + filePath + System.lineSeparator() + e.getMessage());
        }
    }

    //5. ИЗМЕНЕНИЕ УЖЕ ОТПРАВЛЕННОГО СООБЩЕНИЯ
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

    //СОЗДАНИЕ И ВОЗВРАТ Inline-КЛАВИАТУРЫ
    /**
     * Метод создания и возврата Inline-клавиатуры
     * @param inlineKeyboardNames названия кнопок
     * @return Inline-клавиатура
     */
    private InlineKeyboardMarkup getInlineKeyboardMarkup(List<List<String>> inlineKeyboardNames) {
        InlineKeyboardMarkup iKeyboard = new InlineKeyboardMarkup();        //сама клава
        List<List<InlineKeyboardButton>> iRows = new ArrayList<>();         //строки кнопок
        for (var inlineKeyboardNamesRow : inlineKeyboardNames) {
            List<InlineKeyboardButton> iRow = new ArrayList<>();            //строка кнопок
            for(var iName : inlineKeyboardNamesRow){
                InlineKeyboardButton iButton = new InlineKeyboardButton();  //кнопки
                iButton.setText(iName);                                     //дали названия кнопкам
                iButton.setCallbackData(iName+"CD");                        //дали коллбэки кнопкам
                iRow.add(iButton);                                          //добавили кнопки в ряд
            }
            iRows.add(iRow);                                                //добавили ряд в ряды кнопок
        }
        iKeyboard.setKeyboard(iRows);                                       //добавили ряды в клавиатуру
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

}
