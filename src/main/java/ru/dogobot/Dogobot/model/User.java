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

    //region Изменяемые параметры пользователя, которые хранятся не только в БД, но в JSON
    @Column(name = "pack_password")
    private String packPassword;

    @Column(name = "personal_email")
    private String personalEmail;

    @Column(name = "other_email")
    private String otherEmail;
    //endregion

    public User() {
    }
    public User(
            Long id,
            String firstName,
            String lastName,
            String userName,
            Timestamp timestamp,
            String packPassword,
            String personalEmail,
            String otherEmail
    ) {
        this.chatId = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userName = userName;
        this.registeredAt = timestamp;
        this.packPassword = packPassword;
        this.personalEmail = personalEmail;
        this.otherEmail = otherEmail;
    }

    @Override
    public String toString() {
        String sep = System.lineSeparator();
        return "Данные о пользователе." + sep +
                "Id=" + ((chatId == null)? "null" : chatId) + ", " + sep
                + "First Name='" + ((firstName == null)? "null" : firstName) + '\'' + ", " + sep
                + "Last Name='" + ((lastName == null)? "null" : lastName) + '\'' + ", " + sep
                + "User Name='" + ((userName == null)? "null" : userName) + '\'' + ", " + sep
                + "Pack Password='" + ((packPassword == null)? "null" : packPassword) + '\'' + ", " + sep
                + "Personal Email='" + ((personalEmail == null)? "null" : personalEmail) + '\'' + ", " + sep
                + "Other Email='" + ((otherEmail == null)? "null" : otherEmail) + '\'' + ", " + sep
                + "Registered at=" + ((registeredAt == null)? "null" : registeredAt);
    }
}
