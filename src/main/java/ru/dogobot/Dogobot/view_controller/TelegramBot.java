package ru.dogobot.Dogobot.view_controller;

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
import ru.dogobot.Dogobot.model.FileDir;
import ru.dogobot.Dogobot.service.FileManager;

import java.io.*;
import java.util.*;

@Slf4j
@Service
public class TelegramBot extends TelegramLongPollingBot {

    //todo Посмотреть модификаторы доступа

    //region КОНСТАНТЫ и другие исходные данные
    final BotConfig botConfig;

    //команды меню
    @Getter
    @AllArgsConstructor
    public enum BotMenuEnum {
        START("/start", "Здравствуйте!"),
        SHOWHOME("/showhome", "Показать домашнюю папку"),
        GOTO("/goto", "Перейти по адресу"),
        SCREENSHOT("/screenshot", "Сделать скриншот"),
        MYDATA("/mydata", "Посмотреть данные о себе"),
        DELETEDATA("/deletedata", "Удалить данные о себе"),
        HELP("/help", "Помощь"),
        SETTINGS("/settings", "Настройки"),
        RESET("/botreset", "Перезапустить бота"),
        EXIT("/botstop", "Остановить бота");

        private final String key;
        private final String description;
    }

    final List<BotCommand> botMenu;

    //нижняя клавиатура по умолчанию (только наименования в таблице)
    @Getter
    @AllArgsConstructor
    public enum ReplyKeyboardEnum {
        ROW1_BUTTON1("Домой", "Показать домашнюю папку"),
        ROW1_BUTTON2("Настройки", "Настройки");

        private final String key;
        private final String description;
    }

    List<List<String>> replyKeyboardNames = new ArrayList<>(
            List.of(Arrays.asList(ReplyKeyboardEnum.ROW1_BUTTON1.key, ReplyKeyboardEnum.ROW1_BUTTON2.key))
    );

    //другие команды через дробь
    @Getter
    @AllArgsConstructor
    public enum OtherCommandEnum {
        GOTO("/goto ", "Перейти в указанную папку"),
        SETPASS("/setpass ", "Установить новый пароль"),
        SETPMAIL("/setpmail ", "Установить новый адрес 'персональной электронной почты'"),
        SETOMAIL("/setomail ", "Установить новый адрес 'другой электронной почты'"),
        RENAME("/rn ", "Переименовать папку/файл"),
        MOVE("/mv ", "Переместить папку/файл"),
        COPY("/cp ", "Копировать папку/файл"),
        CMD("/cmd ", "Выполнить консольную команду");

        private final String key;
        private final String description;
    }

    private long fileDirMessageId;

    //endregion

    //region AUTOWIRED and CONSTRUCTORS

    @Autowired
    private FileManager fileManager;

    public TelegramBot(BotConfig botConfig) {
        this.botConfig = botConfig;
        this.botMenu = getBotMenu();
    }

    /**
     * Заполняет меню бота командами, считывая их из BotMenuEnum
     *
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

    //region OVERRIDE TelegramBots + фильтр на владельца

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
     *
     * @param update объект обновления
     * @return true - обратился владелец
     */
    private boolean ownerFilter(Update update) {
        boolean result;
        try {
            Long ownerId = botConfig.getOwnerId();
            result = (update.getMessage() != null && update.getMessage().getFrom().getId().equals(ownerId)) ||
                    (update.getCallbackQuery() != null && update.getCallbackQuery().getFrom().getId().equals(ownerId));
        } catch (Exception e) {
            log.error("Не могу определить владельца: " + e.getMessage());
            result = false;
        }
        return result;
    }

    //endregion

    //region Обработчики команд со слешем

    /**
     * Обработчик текстовых сообщений
     *
     * @param update объект обновления
     */
    private void handlerText(Update update) {
        String messageText = update.getMessage().getText();

        if (messageText.equals(BotMenuEnum.START.getKey())) {
            commandStartHandler(update);

        } else if (messageText.equals(BotMenuEnum.SHOWHOME.getKey()) || messageText.equals(ReplyKeyboardEnum.ROW1_BUTTON1.key)) {
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

        } else if (messageText.equals(BotMenuEnum.SETTINGS.getKey()) || messageText.equals(ReplyKeyboardEnum.ROW1_BUTTON2.key)) {
            commandSettingsHandler(update);

        } else if (messageText.equals(BotMenuEnum.RESET.getKey())) {
            commandResetHandler(update);

        } else if (messageText.equals(BotMenuEnum.EXIT.getKey())) {
            commandExitHandler(update);

        } else {
            //варианты команд со слешем, но с аргументами
            if (isCommand(update, OtherCommandEnum.GOTO)) {
                commandGotoArgsHandler(update);

            } else if (isCommand(update, OtherCommandEnum.SETPASS)) {
                commandSetPassHandler(update);

            } else if (isCommand(update, OtherCommandEnum.SETPMAIL)) {
                commandSetPersonalMailHandler(update);

            } else if (isCommand(update, OtherCommandEnum.SETOMAIL)) {
                commandSetOtherMailHandler(update);

            } else if (isCommand(update, OtherCommandEnum.RENAME)) {
                commandRenameHandler(update);

            } else if (isCommand(update, OtherCommandEnum.MOVE)) {
                commandMoveHandler(update);

            } else if (isCommand(update, OtherCommandEnum.COPY)) {
                commandCopyHandler(update);

            } else if (isCommand(update, OtherCommandEnum.CMD)) {
                commandCMDHandler(update);

            } else {
                commandOrElseHandler(update);
            }
        }
    }

    /**
     * Проверяет, является ли сообщение командой из списка других команд
     *
     * @param update  объект обновления
     * @param command команда для проверки - введенный пользователем текст
     * @return true - команда из списка других команд
     */
    boolean isCommand(Update update, OtherCommandEnum command) {
        if (update.getMessage().getText().length() < command.key.length()) return false;
        return update.getMessage().getText().startsWith(command.key);
    }

    //endregion

    //region Обработчики конкретных команд со слешем

    /**
     * Обработчик команды /start
     *
     * @param update объект обновления
     */
    private void commandStartHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        String report = fileManager.findUserOrRegister(update);
        sendMessageWithReplyKeyboard(chatId, report);
    }

    /**
     * Обработчик команды /showdir
     *
     * @param update объект обновления
     */
    private void commandShowhomeHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        FileDir fileDir = fileManager.getFileDirHomeWithScan();
        sendMessageWithInlineKeyboard(
                chatId,
                "%s: %s".formatted("Текущий путь ", fileDir.getFdPath()),
                fileDir
        );
    }

    /**
     * Обработчик команды /goto
     *
     * @param update объект обновления
     */
    private void commandGotoHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        sendMessageWithoutKeyboard(
                chatId,
                "Для того, чтоб напрямую перейти по адресу, введите команду (без кавычек и фигурных скобок) в формате '" + OtherCommandEnum.GOTO.key + " {путь для перехода}'"
        );
    }

    /**
     * Обработчик команды /goto с параметром (путём для перехода)
     *
     * @param update объект обновления
     */
    private void commandGotoArgsHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        String targetPath = update.getMessage().getText()
                .substring(OtherCommandEnum.GOTO.key.length()).trim();
        FileDir fileDir = fileManager.getFileDirWithScan(targetPath);
        if (fileDir == null) {
            String report = "Путь не найден: " + targetPath;
            log.error(report);
            sendMessageWithoutKeyboard(chatId, report);
            return;
        }
        sendMessageWithInlineKeyboard(
                chatId,
                "%s: %s".formatted("Текущий путь ", fileDir.getFdPath()),
                fileDir
        );
    }

    /**
     * Обработчик команды /setpass
     *
     * @param update объект обновления
     */
    private void commandSetPassHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        String report = fileManager.updatePackPassword(
                update,
                update.getMessage().getText()
                        .substring(OtherCommandEnum.SETPASS.key.length())
        );
        sendMessageWithoutKeyboard(chatId, report);
    }

    /**
     * Обработчик команды /setpmail
     *
     * @param update объект обновления
     */
    private void commandSetPersonalMailHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        String report = fileManager.updatePersonalMail(
                update,
                update.getMessage().getText()
                        .substring(OtherCommandEnum.SETPMAIL.key.length()).trim()
        );
        sendMessageWithoutKeyboard(chatId, report);
    }

    /**
     * Обработчик команды /setomail
     *
     * @param update объект обновления
     */
    private void commandSetOtherMailHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        String report = fileManager.updateOtherMail(
                update,
                update.getMessage().getText()
                        .substring(OtherCommandEnum.SETOMAIL.key.length()).trim()
        );
        sendMessageWithoutKeyboard(chatId, report);
    }

    /**
     * Обработчик команды /screenshot
     *
     * @param update объект обновления
     */
    private void commandScreenshotHandler(Update update) {
        String report;
        String screenPath = fileManager.printScreen();
        if (screenPath == null) {
            report = "Не удалось создать скриншот.";
            log.error(report);
            sendMessageWithoutKeyboard(update.getMessage().getChatId(), report);
            return;
        }
        report = "Скриншот сделан. " + sendFile(update.getMessage().getChatId(), screenPath);
        sendMessageWithoutKeyboard(update.getMessage().getChatId(), report);
    }

    /**
     * Обработчик команды /mydata
     *
     * @param update объект обновления
     */
    private void commandMydataHandler(Update update) {
        String report = fileManager.getUserInfo(update);
        sendMessageWithoutKeyboard(update.getMessage().getChatId(), report);
    }

    /**
     * Обработчик команды /deletedata
     *
     * @param update объект обновления
     */
    private void commandDeletedataHandler(Update update) {
        String report = fileManager.deleteUser(update);
        //todo добавить полную очистку чата и оставить кнопку /start
        sendMessageWithoutKeyboard(update.getMessage().getChatId(), report);
    }

    /**
     * Обработчик команды /help
     *
     * @param update объект обновления
     */
    private void commandHelpHandler(Update update) {
        //todo доделать этот метод
        sendMessageWithoutKeyboard(update.getMessage().getChatId(), "Инструкция пока пишется.");
    }

    /**
     * Обработчик команды /settings
     *
     * @param update объект обновления
     */
    private void commandSettingsHandler(Update update) {
        String sep = System.lineSeparator();
        String report = fileManager.getUserSettings(update) +
                "---" + sep +
                "Для изменения пароля введите команду (без кавычек и фигурных скобок)" + sep +
                "в формате: '" + OtherCommandEnum.SETPASS.key + "{новый пароль}'." + sep +
                "Например: " + OtherCommandEnum.SETPASS.key + "1111" + sep +
                "---" + sep +
                "Для изменения личной почты (для получения на неё писем) введите команду (без кавычек и фигурных скобок)" + sep +
                "в формате '" + OtherCommandEnum.SETPMAIL.key + "{новый адрес личной почты}'." + sep +
                "Например: " + OtherCommandEnum.SETPMAIL.key + "mynew@personal.mail" + sep +
                "---" + sep +
                "Для изменения другой почты (для отправки на неё писем) введите команду (без кавычек и фигурных скобок)" + sep +
                "в формате '" + OtherCommandEnum.SETOMAIL.key + "{новый адрес другой почты}'." + sep +
                "Например: " + OtherCommandEnum.SETOMAIL.key + "mynew@other.mail" + sep +
                "Проверьте, чтоб между командой и новым значением был всего один пробел.";
        sendMessageWithoutKeyboard(update.getMessage().getChatId(), report);
    }

    /**
     * Обработчик команды /reset
     *
     * @param update объект обновления
     */
    private void commandResetHandler(Update update) {
        String report = "Перезапускаю бота, подождите 5-10 секунд...";
        sendMessageWithoutKeyboard(update.getMessage().getChatId(), report);
        log.info(report);
        fileManager.botReset();
    }

    /**
     * Обработчик команды /exit
     *
     * @param update объект обновления
     */
    private void commandExitHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        String report = "Всё, выключаю бота на том устройстве";
        sendMessageWithoutKeyboard(chatId, report);
        log.info(report);
        System.exit(0);
    }

    //варианты с дробью, но с аргументами

    /**
     * Обработчик команды OtherCommandEnum.RENAME
     *
     * @param update объект обновления
     */
    private void commandRenameHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        String report = fileManager.fileDirRename(
                fileManager.getFileDir(),
                update.getMessage().getText()
                        .substring(OtherCommandEnum.RENAME.key.length()).trim()
        );
        sendMessageWithoutKeyboard(chatId, report);
    }

    /**
     * Обработчик команды OtherCommandEnum.MOVE
     *
     * @param update объект обновления
     */
    private void commandMoveHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        String report = fileManager.fileDirMove(
                fileManager.getFileDir(),
                update.getMessage().getText()
                        .substring(OtherCommandEnum.MOVE.key.length()).trim()
        );
        sendMessageWithoutKeyboard(chatId, report);
    }

    /**
     * Обработчик команды OtherCommandEnum.COPY
     *
     * @param update объект обновления
     */
    private void commandCopyHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        String report = fileManager.fileDirCopy(
                fileManager.getFileDir(),
                update.getMessage().getText()
                        .substring(OtherCommandEnum.COPY.key.length()).trim()
        );
        sendMessageWithoutKeyboard(chatId, report);
    }

    /**
     * Обработчик команды OtherCommandEnum.CMD
     *
     * @param update объект обновления
     */
    private void commandCMDHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        String report = fileManager.terminalExecute(
                update.getMessage().getText()
                        .substring(OtherCommandEnum.CMD.key.length()).trim()
        );
        sendMessageWithoutKeyboard(chatId, report);
    }

    /**
     * Обработчик команд не из списка
     *
     * @param update объект обновления
     */
    private void commandOrElseHandler(Update update) {
        long chatId = update.getMessage().getChatId();
        sendMessageWithoutKeyboard(chatId, "Мне неизвестно что делать с командой: " + update.getMessage().getText());
    }

    //endregion

    //region Обработчики Callback

    /**
     * Обработчик команд Callback
     *
     * @param update объект обновления
     */
    private void handlerCallBackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        long messageId = update.getCallbackQuery().getMessage().getMessageId();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        // если callbackData - один из пунктов словаря актуальной папки
        if (fileManager.getCurrentPathDict().containsKey(callbackData)) {
            FileDir targetFileDir = fileManager.getCurrentPathDict().get(callbackData);

            //если нажали на Inline-кнопку МЕНЮ
            if (targetFileDir.getFdNameInline().equals(fileManager.getMENU())) {
                sendMessageWithInlineFileDirMenu(
                        chatId,
                        FileManager.SELECT_MENU_ITEM,
                        FileManager.FileDirMenu.values()
                );
                return;
            }

            //если нажали на кнопку на файл или папку
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
        if (isCallbackData(callbackData, FileManager.FileDirMenu.GET_INFO)) {
            sendMessageWithoutKeyboard(
                    chatId,
                    fileManager.getFileDir().toString()
            );
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали на кнопку "Получить в телеграм"
        if (isCallbackData(callbackData, FileManager.FileDirMenu.GET_ON_TELEGRAM)) {
            String report;
            if (fileManager.getFileDir().getFdType().equals(FileDir.FDType.FILE)) {
                report = sendFile(chatId, fileManager.getFileDir().getFdPath());
            } else {
                report = "Это папка, для отправки в телеграм, её сначала нужно упаковать в архив.";
                log.warn(report);
            }
            sendMessageWithoutKeyboard(chatId, report);
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали на кнопку "Получить на почту"
        if (isCallbackData(callbackData, FileManager.FileDirMenu.GET_ON_EMAIL)) {
            String report = fileManager.sendEmailPersonal(
                    fileManager.getFileDir(),
                    update
            );
            sendMessageWithoutKeyboard(chatId, report);
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали на кнопку "Отправить на почту"
        if (isCallbackData(callbackData, FileManager.FileDirMenu.SEND_TO_EMAIL)) {
            String report = fileManager.sendEmailOther(
                    fileManager.getFileDir(),
                    update
            );
            sendMessageWithoutKeyboard(chatId, report);
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали на кнопку "Упаковать в zip"
        if (isCallbackData(callbackData, FileManager.FileDirMenu.PACK)) {
            String report = fileManager.zipFileDirWithoutPassword(
                    fileManager.getFileDir()
            );
            sendMessageWithoutKeyboard(chatId, report);
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали на кнопку "Упаковать в zip с паролем"
        if (isCallbackData(callbackData, FileManager.FileDirMenu.PACK_WITH_PASSWORD)) {
            String report = fileManager.zipFileDirWithPassword(
                    fileManager.getFileDir(),
                    update
            );
            sendMessageWithoutKeyboard(chatId, report);
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали "Распаковать из zip"
        if (isCallbackData(callbackData, FileManager.FileDirMenu.UNPACK)) {
            String report = fileManager.unzipFileDirWithoutPassword(
                    fileManager.getFileDir()
            );
            sendMessageWithoutKeyboard(chatId, report);
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали "Распаковать из zip с паролем"
        if (isCallbackData(callbackData, FileManager.FileDirMenu.UNPACK_WITH_PASSWORD)) {
            String report = fileManager.unzipFileDirWithPassword(
                    fileManager.getFileDir(),
                    update
            );
            sendMessageWithoutKeyboard(chatId, report);
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали на кнопку "Переименовать"
        if (isCallbackData(callbackData, FileManager.FileDirMenu.RENAME)) {
            sendMessageWithoutKeyboard(
                    chatId,
                    "Для переименования текущего файла/папки введите команду (без кавычек и фигурных скобок) в формате '" + OtherCommandEnum.RENAME.key + " {новое имя}'"
            );
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали кнопку "Переместить"
        if (isCallbackData(callbackData, FileManager.FileDirMenu.MOVE)) {
            sendMessageWithoutKeyboard(
                    chatId,
                    "Для перемещения текущего файла/папки введите команду (без кавычек и фигурных скобок) в формате '" + OtherCommandEnum.MOVE.key + " {новый путь к папке для перемещения}'"
            );
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали кнопку "Копировать"
        if (isCallbackData(callbackData, FileManager.FileDirMenu.COPY)) {
            sendMessageWithoutKeyboard(
                    chatId,
                    "Для копирования текущего файла/папки введите команду (без кавычек и фигурных скобок) в формате '" + OtherCommandEnum.COPY.key + " {новый путь к папке для копирования}'"
            );
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали на кнопку "Удалить"
        if (isCallbackData(callbackData, FileManager.FileDirMenu.DELETE)) {
            String report = fileManager.fileDirDelete(
                    fileManager.getFileDir()
            );
            sendMessageWithoutKeyboard(chatId, report);
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        //если нажали на кнопку "Убрать меню"
        if (isCallbackData(callbackData, FileManager.FileDirMenu.REMOVE_MENU)) {
            deleteMessageWithFileDirMenu(chatId);
            return;
        }

        String report = "Не могу распознать нажатую кнопку " + callbackData;
        log.error(report + callbackData);
        sendMessageWithoutKeyboard(chatId, report);
    }

    /**
     * Метод для проверки нажатой кнопки на callbackData
     * @param callbackData нажатая кнопка
     * @param fileDirMenu меню callbackData-констант
     * @return true если нажатая кнопка - callbackData из меню
     */
    private boolean isCallbackData(String callbackData, FileManager.FileDirMenu fileDirMenu) {
        boolean result = false;
        try {
            result = (!callbackData.isBlank())
            && (!fileDirMenu.getButtonCallback().isBlank())
            && callbackData.equals(fileDirMenu.getButtonCallback());
        } catch (Exception e) {
            log.error("Не удалось распознать нажатую кнопку. " + System.lineSeparator() + e.getMessage());
        }
        return result;
    }

    //endregion

    //region ОТПРАВКА И ИЗМЕНЕНИЕ СООБЩЕНИЙ

    //1. ПРОСТАЯ ОТПРАВКА СООБЩЕНИЯ

    /**
     * Метод для отправки текстового сообщения без вызова клавиатур
     *
     * @param chatId      Id чата получателя
     * @param textMessage текстовое сообщение
     */
    private void sendMessageWithoutKeyboard(long chatId, String textMessage) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(textMessage);
            executeMessage(message);
        } catch (Exception e) {
            log.error("Не удалось отправить сообщение: " + e.getMessage());
        }
    }

    //2. ОТПРАВКА СООБЩЕНИЯ С КЛАВИАТУРОЙ

    /**
     * Метод для отправки текстового сообщения с вызовом Reply-клавиатуры
     *
     * @param chatId      Id чата получателя
     * @param textMessage текстовое сообщение
     */
    private void sendMessageWithReplyKeyboard(long chatId, String textMessage) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(textMessage);

            ReplyKeyboardMarkup keyboardMarkup = getReplyKeyboardMarkup();
            message.setReplyMarkup(keyboardMarkup);

            executeMessage(message);
        } catch (Exception e) {
            log.error("Не удалось отправить сообщение: " + e.getMessage());
        }
    }

    //3. ОТПРАВКА СООБЩЕНИЯ С ИНЛАЙН-КЛАВИАТУРОЙ

    /**
     * Метод для отправки текстового сообщения с вызовом Inline-клавиатуры (на основе элемента файловой системы)
     *
     * @param chatId      Id чата получателя
     * @param textMessage текстовое сообщение
     * @param fileDir     элемент файловой системы, содержащий информацию для создания клавиатуры (названия кнопок, команды и т.п.)
     */
    private void sendMessageWithInlineKeyboard(long chatId, String textMessage, FileDir fileDir) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(textMessage);
            InlineKeyboardMarkup markupInLine = getInlineKeyboardMarkup(fileDir);
            message.setReplyMarkup(markupInLine);

            executeMessage(message);
        } catch (Exception e) {
            log.error("Не удалось отправить сообщение: " + e.getMessage());
        }
    }

    /**
     * Метод для отправки текстового сообщения с вызовом Inline-клавиатуры (на основе переданного массива из именованных констант)
     *
     * @param chatId                     Id чата получателя
     * @param textMessage                текстовое сообщение
     * @param fileDirMenuSortedCallbacks массив из именованных констант, содержащий как Callback-команды, так и названия кнопок
     *                                   (предоставляет класс файлового менеджера)
     */
    private void sendMessageWithInlineFileDirMenu(long chatId, String textMessage, FileManager.FileDirMenu[] fileDirMenuSortedCallbacks) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(textMessage);
            InlineKeyboardMarkup markupInLine = getInlineKeyboardMarkup(fileDirMenuSortedCallbacks);
            message.setReplyMarkup(markupInLine);

            this.fileDirMessageId = executeMessage(message);
        } catch (Exception e) {
            log.error("Не удалось отправить сообщение: " + e.getMessage());
        }
    }

    /**
     * Метод для отправки подготовленного сообщением в чат
     *
     * @param message подготовленное сообщение
     */
    private Integer executeMessage(SendMessage message) {
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
     *
     * @param chatId   Id чата получателя
     * @param filePath путь к отправляемому файлу
     * @return отчет о выполнении
     */
    private String sendFile(long chatId, String filePath) {
        String report;
        try {
            execute(new SendDocument(String.valueOf(chatId), new InputFile(new File(filePath))));
            report = "Файл " + filePath + " отправлен в чат " + chatId;
            log.info(report);
        } catch (Exception e) {
            report = "Не удалось отправить файл " + filePath + " в чат " + chatId;
            log.error(report + System.lineSeparator() + e.getMessage());
        }
        return report;
    }

    //5. ИЗМЕНЕНИЕ УЖЕ ОТПРАВЛЕННОГО СООБЩЕНИЯ
    //  1ая перегрузка

    /**
     * Метод для изменения уже отправленного сообщения без добавления клавиатур
     *
     * @param chatId      Id чата получателя
     * @param messageId   Id сообщения
     * @param textMessage текстовое сообщение для замены старого сообщения
     */
    private void editMessageWithoutKeyboard(long chatId, long messageId, String textMessage) {
        try {
            EditMessageText message = new EditMessageText();
            message.setChatId(String.valueOf(chatId));
            message.setText(textMessage);
            message.setMessageId((int) messageId);

            execute(message);
            log.info("Сообщение с id " + messageId + " в чате " + chatId + " изменено");
        } catch (Exception e) {
            log.error("Не удалось изменить сообщение с id " + messageId + " в чате: " + e.getMessage());
        }
    }

    //  2ая перегрузка

    /**
     * Метод для изменения уже отправленного сообщения с добавлением Inline-клавиатуры
     *
     * @param chatId      Id чата получателя
     * @param messageId   Id сообщения
     * @param textMessage текстовое сообщение для замены старого сообщения
     * @param fileDir     элемент файловой системы, содержащий информацию для создания клавиатуры (названия кнопок, команды и т.п.)
     */
    private void editMessageWithInlineKeyboard(long chatId, long messageId, String textMessage, FileDir fileDir) {
        try {
            EditMessageText message = new EditMessageText();
            message.setChatId(String.valueOf(chatId));
            message.setText(textMessage);
            message.setMessageId((int) messageId);

            InlineKeyboardMarkup markupInLine = getInlineKeyboardMarkup(fileDir);
            message.setReplyMarkup(markupInLine);

            execute(message);
            log.info("Сообщение с id " + messageId + " в чате " + chatId + " изменено");
        } catch (Exception e) {
            log.error("Не удалось изменить сообщение с id " + messageId + " в чате: " + e.getMessage());
        }
    }

    //6. УДАЛЕНИЕ УЖЕ ОТПРАВЛЕННОГО СООБЩЕНИЯ С IНЛАЙН-КЛАВИАТУРОЙ

    /**
     * Метод для удаления уже отправленного сообщения с добавлением Inline-клавиатуры
     * @param chatIdLong Id чата
     */
    private void deleteMessageWithFileDirMenu(long chatIdLong) {
        Integer messageId = null;
        try {
            messageId = (int) this.fileDirMessageId;
            String chatId = String.valueOf(chatIdLong);
            DeleteMessage deleteMessage = new DeleteMessage(chatId, messageId);
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
     *
     * @param fileDir элемент файловой системы, содержащий информацию для создания клавиатуры (названия кнопок, команды и т.п.)
     * @return Inline-клавиатура
     */
    private InlineKeyboardMarkup getInlineKeyboardMarkup(FileDir fileDir) {
        InlineKeyboardMarkup iKeyboard = new InlineKeyboardMarkup();
        try {
            List<List<InlineKeyboardButton>> iRows = new ArrayList<>();
            for (var inlineKeyboardIdsRow : fileDir.getFdInlineKeyboardIds()) {
                List<InlineKeyboardButton> iRow = new ArrayList<>();
                for (var inlineKeyboardId : inlineKeyboardIdsRow) {
                    InlineKeyboardButton iButton = new InlineKeyboardButton();
                    FileDir fileDirInFolder = fileManager.getCurrentPathDict().get(inlineKeyboardId);
                    iButton.setText(fileDirInFolder.getFdNameInline());
                    iButton.setCallbackData(fileDirInFolder.getFdCallbackData());
                    iRow.add(iButton);
                }
                iRows.add(iRow);
            }
            iKeyboard.setKeyboard(iRows);
        } catch (Exception e) {
            log.error("Не удалось создать Inline-клавиатуру: " + e.getMessage());
        }
        return iKeyboard;
    }

    //      2ая перегрузка - для меню элементов файловой системы

    /**
     * Метод создания и возврата Inline-клавиатуры на основе переданного массива из именованных констант.
     *
     * @param fileDirMenuSortedCallbacks массив из именованных констант, содержащий как Callback-команды, так и названия кнопок
     *                                   (предоставляет класс файлового менеджера)
     * @return Inline-клавиатура
     */
    private InlineKeyboardMarkup getInlineKeyboardMarkup(FileManager.FileDirMenu[] fileDirMenuSortedCallbacks) {
        InlineKeyboardMarkup iKeyboard = new InlineKeyboardMarkup();
        try {
            List<List<InlineKeyboardButton>> iRows = new ArrayList<>();

            for (FileManager.FileDirMenu fileDirMenuSortedCallback : fileDirMenuSortedCallbacks) {
                List<InlineKeyboardButton> iRow = new ArrayList<>();
                InlineKeyboardButton iButton = new InlineKeyboardButton();
                iButton.setText(fileDirMenuSortedCallback.getButtonText());
                iButton.setCallbackData(fileDirMenuSortedCallback.getButtonCallback());
                iRow.add(iButton);
                iRows.add(iRow);
            }
            iKeyboard.setKeyboard(iRows);
        } catch (Exception e) {
            log.error("Не удалось создать Inline-клавиатуру: " + e.getMessage());
        }
        return iKeyboard;
    }

    /**
     * Метод создания и возврата Reply-клавиатуры по умолчанию
     *
     * @return Reply-клавиатура
     */
    private ReplyKeyboardMarkup getReplyKeyboardMarkup() {
        return getReplyKeyboardMarkup(this.replyKeyboardNames);
    }

    /**
     * Метод создания и возврата Reply-клавиатуры
     *
     * @param replyKeyboardNames названия кнопок
     * @return Reply-клавиатура
     */
    private ReplyKeyboardMarkup getReplyKeyboardMarkup(List<List<String>> replyKeyboardNames) {
        ReplyKeyboardMarkup rKeyboard = new ReplyKeyboardMarkup();
        try {
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
        } catch (Exception e) {
            log.error("Не удалось создать Reply-клавиатуру: " + e.getMessage());
        }
        return rKeyboard;
    }

    //endregion

}
