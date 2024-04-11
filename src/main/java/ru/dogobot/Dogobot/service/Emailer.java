package ru.dogobot.Dogobot.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.springframework.stereotype.Service;
import ru.dogobot.Dogobot.config.EmailConfig;

import javax.mail.*;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

@Slf4j
@Service
public class Emailer {
    final EmailConfig emailConfig;

    public Emailer(EmailConfig emailConfig) {
        this.emailConfig = emailConfig;
    }

    /**
     * Отправляет письмо без вложения
     * @param recipient адрес получателя
     * @param subject тема письма
     * @param body текст письма
     * @throws EmailException исключение, возникшее при отправке
     */
    public void sendEmailWithoutAttachment(String recipient, String subject, String body) throws EmailException {
        MultiPartEmail email = new MultiPartEmail();
        email.setHostName(emailConfig.getSmtpHost());
        email.setSmtpPort(emailConfig.getSmtpPort());
        email.setAuthenticator(new DefaultAuthenticator(emailConfig.getEmail(), emailConfig.getPassword()));
        email.setSSLOnConnect(true);
        email.setFrom(emailConfig.getEmail());
        email.addTo(recipient);
        email.setSubject(subject);
        email.setMsg(body);
        email.send();
    }

    /**
     * Отправляет письмо с вложением
     * @param recipient адрес получателя
     * @param subject тема письма
     * @param body текст письма
     * @param attachmentPath путь к вложению
     * @throws EmailException исключение, возникшее при отправке
     */
    public void sendEmailWithAttachment(String recipient, String subject, String body, String attachmentPath) throws EmailException {
        MultiPartEmail email = new MultiPartEmail();
        email.setHostName(emailConfig.getSmtpHost());
        email.setSmtpPort(emailConfig.getSmtpPort());
        email.setAuthenticator(new DefaultAuthenticator(emailConfig.getEmail(), emailConfig.getPassword()));
        email.setSSLOnConnect(true);
        email.setFrom(emailConfig.getEmail());
        email.addTo(recipient);
        email.setSubject(subject);
        email.setMsg(body);

        EmailAttachment attachment = new EmailAttachment();
        attachment.setPath(attachmentPath);
        attachment.setDisposition(EmailAttachment.ATTACHMENT);
        email.attach(attachment);

        email.send();
    }

    /**
     * Получает письмо с вложением и сохраняет его в указанную директорию
     * @param dirPathForAttachment путь к директории. ВАЖНО, чтоб в конце был слеш
     * @throws Exception исключения, которые могут возникать при получении
     */
    public void receiveEmailWithAttachment(String dirPathForAttachment) throws Exception {
        //todo продумать стратегию использования (спец.папка на сервере и прочее)

        // Настройка свойств JavaMail
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imaps"); // Используем протокол IMAP для получения почты
        props.setProperty("mail.imaps.host", this.emailConfig.getImapHost());

        // Создание сеанса
        Session session = Session.getDefaultInstance(props);

        // Создание хранилища
        Store store = session.getStore("imaps");
        store.connect(this.emailConfig.getImapHost(), this.emailConfig.getEmail(), this.emailConfig.getPassword());

        // Открытие папки входящих сообщений
        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);

        // Поиск сообщений с вложениями
        Message[] messages = inbox.getMessages();
        for (Message message : messages) {
            if (message.getContent() instanceof Multipart) {
                Multipart multipart = (Multipart) message.getContent();
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                        // Обработка вложения
                        String fileName = bodyPart.getFileName();
                        // Сохранение вложения
                        InputStream inputStream = bodyPart.getInputStream();
                        OutputStream outputStream = new FileOutputStream(dirPathForAttachment + fileName);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                        outputStream.close();
                        inputStream.close();
                    }
                }
            }
        }

        // Закрытие папки и хранилища
        inbox.close(false);
        store.close();
    }
}

/*
    // Метод для отправки электронной почты с вложением
    public void sendEmailWithAttachment(String to, String subject, String body, String attachmentPath) throws MessagingException {
        // Настройка свойств JavaMail
        Properties props = new Properties();
        props.put("mail.smtp.host", this.emailConfig.getSmtpHost());
        props.put("mail.smtp.port", this.emailConfig.getSmtpPort());
//        props.put("mail.smtp.auth", "true");
//        props.put("mail.smtp.starttls.enable", "true");
//        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        // Создание сеанса
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                String username = EmailExample.this.emailConfig.getEmail();
                String password = EmailExample.this.emailConfig.getPassword();
                return new PasswordAuthentication(username, password);
            }
        });

        // Создание сообщения
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(this.emailConfig.getEmail()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);

        // Создание Multipart объекта
        Multipart multipart = new MimeMultipart();

        // Добавление текстовой части сообщения
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText(body);
        multipart.addBodyPart(messageBodyPart);

        // Добавление вложения
        messageBodyPart = new MimeBodyPart();
        DataSource source = new FileDataSource(attachmentPath);
        messageBodyPart.setDataHandler(new DataHandler(source));
        messageBodyPart.setFileName(attachmentPath);
        multipart.addBodyPart(messageBodyPart);

        // Установка содержимого сообщения
        message.setContent(multipart);

        // Отправка сообщения
        Transport.send(message);
    }
*/