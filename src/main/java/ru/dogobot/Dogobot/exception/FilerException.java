package ru.dogobot.Dogobot.exception;

public class FilerException extends Exception {
    /**
     * Исключение, возникающее при работе с файловой системой.
     * @param message сообщение исключения
     */
    public FilerException(String message) {
        super(message);
    }
}