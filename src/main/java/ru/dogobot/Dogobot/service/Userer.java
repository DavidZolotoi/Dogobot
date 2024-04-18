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

    protected User findUserById(Long chatId) throws NoSuchElementException {

        User user = userRepository.findById(chatId).get();

        return user;
    }

    protected User registerUser(User userForRegister) throws Exception {

        User user = userRepository.save(userForRegister);

        return user;
    }

    protected User deleteUser(User user) throws Exception {

        userRepository.delete(user);

        return user;
    }

    protected User updatePackPassword(User user, String newPackPassword) throws Exception {

        user.setPackPassword(newPackPassword);
        userRepository.save(user);

        return user;
    }

    protected User updateOtherMail(User user, String newOtherMail) throws Exception {

        user.setOtherEmail(newOtherMail);
        userRepository.save(user);

        return user;
    }

}
