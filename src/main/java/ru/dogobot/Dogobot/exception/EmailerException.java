package ru.dogobot.Dogobot.exception;

public class EmailerException extends Exception {
    /**
     * Исключение, возникающее при работе с электронной почтой
     * @param message сообщение исключения
     */
    public EmailerException(String message) {
        super(message);
    }
}
