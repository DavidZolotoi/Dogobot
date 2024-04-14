package ru.dogobot.Dogobot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.sql.Timestamp;

@Data
@Entity
@Table(name = "usersDataTable")
public class User {

    @Id
    @Column(name = "chat_id")
    private Long chatId;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "user_name")
    private String userName;
    @Column(name = "registered_at")
    private Timestamp registeredAt;

    //личные настройки
    @Column(name = "pack_password")
    private String packPassword;
    @Column(name = "personal_email")
    private String personalEmail;
    @Column(name = "other_email")
    private String otherEmail;

    @Override
    public String toString() {
        String sep = System.lineSeparator();
        //todo выводить только то, что не null
        return "Данные о пользователе." + sep +
                "Id=" + chatId + ", " + sep
                + "First Name='" + firstName + '\'' + ", " + sep
                + "Last Name='" + lastName + '\'' + ", " + sep
                + "User Name='" + userName + '\'' + ", " + sep
                + "Pack Password='" + packPassword + '\'' + ", " + sep
                + "Personal Email='" + personalEmail + '\'' + ", " + sep
                + "Other Email='" + otherEmail + '\'' + ", " + sep
                + "Registered at=" + registeredAt;
    }
}
