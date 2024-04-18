package ru.dogobot.Dogobot.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.util.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@Configuration
@EnableScheduling
@Data
public class BotConfig {

    @Value("${telegrambot.token}")
    String token;

    @Value("${telegrambot.name}")
    String botName;

    @Value("${telegrambot.authorId}")
    Long authorId;

    @Value("${telegrambot.ownerId}")
    Long ownerId;   //todo может в Json его?

}
