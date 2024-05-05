package ru.dogobot.Dogobot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.dogobot.Dogobot.view_controller.TelegramBot;


@Slf4j
@Component
public class BotInitializer {
    @Autowired
    TelegramBot bot;

    /**
     * Инициализация бота при запуске
     * @throws TelegramApiException если возникли проблемы
     */
    @EventListener({ContextRefreshedEvent.class})
    public void init() throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            telegramBotsApi.registerBot(bot);
            log.info("Бот успешно инициализирован");
        }
        catch (TelegramApiException e) {
            log.error("Проблемы при инициализации бота: " + e.getMessage());
        }
    }
}
