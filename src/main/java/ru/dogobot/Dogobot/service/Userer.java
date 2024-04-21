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

    //todo перепроверить возвраты из методов, возможно в них стоит обратиться к БД

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

        User user = userRepository.findById(chatId).get();

        return user;
    }

    /**
     * Регистрирует пользователя в базе данных
     * @param userForRegister пользователь
     * @return пользователь для регистрации
     * @throws Exception если возникли исключения
     */
    protected User registerUser(User userForRegister) throws Exception {

        User user = userRepository.save(userForRegister);

        return user;
    }

    /**
     * Удаляет пользователя из базы данных
     * @param user пользователь, которого нужно удалить
     * @return пользователь, который был удален
     * @throws Exception если возникли исключения
     */
    protected User deleteUser(User user) throws Exception {

        userRepository.delete(user);

        return user;
    }

    /**
     * Обновляет пароль пользователя в базе данных
     * @param user пользователь, пароль которого нужно обновить
     * @param newPackPassword новый пароль
     * @return пользователь
     * @throws Exception если возникли исключения
     */
    protected User updatePackPassword(User user, String newPackPassword) throws Exception {

        userConfig.updatePackPassword(newPackPassword);
        user.setPackPassword(userConfig.getConfigs().get(userConfig.getPACK_PASSWORD_KEY()));
        userRepository.save(user);

        return user;
    }

    /**
     * Обновляет другой адрес электронной почты пользователя
     * @param user пользователь, другой адрес электронной почты которого нужно обновить
     * @param newPersonalMail новый другой адрес электронной почты
     * @return пользователь
     * @throws Exception если возникли исключения
     */
    protected User updatePersonalEmail(User user, String newPersonalMail) throws Exception {

        userConfig.updatePersonalEmail(newPersonalMail);
        user.setPersonalEmail(userConfig.getConfigs().get(userConfig.getPERSONAL_EMAIL_KEY()));
        userRepository.save(user);

        return user;
    }

    /**
     * Обновляет другой адрес электронной почты пользователя
     * @param user пользователь, другой адрес электронной почты которого нужно обновить
     * @param newOtherMail новый другой адрес электронной почты
     * @return пользователь
     * @throws Exception если возникли исключения
     */
    protected User updateOtherEmail(User user, String newOtherMail) throws Exception {

        userConfig.updateOtherEmail(newOtherMail);
        user.setOtherEmail(userConfig.getConfigs().get(userConfig.getOTHER_EMAIL_KEY()));
        userRepository.save(user);

        return user;
    }

}