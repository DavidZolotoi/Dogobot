package ru.dogobot.Dogobot.service;

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.dogobot.Dogobot.config.UserConfig;
import ru.dogobot.Dogobot.model.User;
import ru.dogobot.Dogobot.model.UserRepository;

import java.util.NoSuchElementException;

@Slf4j
@Getter
@Service
@Data
public class Userer {

    @Autowired
    private UserRepository userRepository;

    private UserConfig userConfig;

    public Userer(UserConfig userConfig) {
        this.userConfig = userConfig;
    }

    /**
     * Ищет пользователя в базе данных по chatId
     * @param chatId Id чата
     * @return найденный пользователь
     * @throws NoSuchElementException если пользователь не найден
     */
    protected User findUserById(Long chatId) throws NoSuchElementException {
        return userRepository.findById(chatId).get();
    }

    /**
     * Регистрирует пользователя в базе данных
     * @param userForRegister пользователь
     * @return пользователь для регистрации
     */
    protected User registerUser(User userForRegister) {
        return userRepository.save(userForRegister);
    }

    /**
     * Удаляет пользователя из базы данных
     * @param chatId Id чата
     */
    protected void deleteUser(Long chatId) {
        User user = findUserById(chatId);
        userRepository.delete(user);
        user = null;
    }

    /**
     * Обновляет пароль пользователя в базе данных
     * @param chatId Id чата
     * @param newPackPassword новый пароль
     * @return пользователь
     */
    protected User updatePackPassword(Long chatId, String newPackPassword) {
        User user = findUserById(chatId);
        //в конфигурациях
        if (!userConfig.updateConfigPackPassword(newPackPassword)) {
            return user;
        }
        //в экземпляре user
        user.setPackPassword(userConfig.getConfigs().get(userConfig.getPACK_PASSWORD_KEY()));
        //в БД
        userRepository.save(user);
        log.info("Пароль пользователя обновлен, как локально, так и в БД.");
        return user;
    }

    /**
     * Обновляет персональный адрес электронной почты пользователя
     * @param chatId Id чата
     * @param newPersonalMail новый персональный адрес электронной почты
     * @return пользователь
     */
    protected User updatePersonalEmail(Long chatId, String newPersonalMail) {
        User user = findUserById(chatId);
        //в конфигурациях
        if(!userConfig.updateConfigPersonalEmail(newPersonalMail)) {
            return user;
        }
        //в экземпляре user
        user.setPersonalEmail(userConfig.getConfigs().get(userConfig.getPERSONAL_EMAIL_KEY()));
        //в БД
        userRepository.save(user);
        log.info("Персональный адрес пользователя обновлен, как локально, так и в БД.");
        return user;
    }

    /**
     * Обновляет другой адрес электронной почты пользователя
     * @param chatId Id чата
     * @param newOtherMail новый другой адрес электронной почты
     * @return пользователь
     */
    protected User updateOtherEmail(Long chatId, String newOtherMail) {
        User user = findUserById(chatId);
        //в конфигурациях
        if (!userConfig.updateConfigOtherEmail(newOtherMail)) {
            return user;
        }
        //в экземпляре user
        user.setOtherEmail(userConfig.getConfigs().get(userConfig.getOTHER_EMAIL_KEY()));
        //в БД
        userRepository.save(user);
        log.info("Другой другой адрес электронной почты пользователя обновлен, как локально, так и в БД.");
        return user;
    }

}
