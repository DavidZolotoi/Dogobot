# Dogobot – удаленный помощник.

Это многофункциональный Telegram-бот, обеспечивающий  удаленный доступ к файловой системе на удалённом устройстве, а также обладающий возможностями выполнять действия на нём. На этапе написания проекта, основной функционал чат-бота предусматривает: 
* основные функции файлового менеджера на удаленном устройстве:
  * навигация по файловой системе,
  * получение информации о файле или папке (далее ф.п.),
  * переименование ф.п.,
  * перемещение ф.п.,
  * копирование ф.п.,
  * удаление ф.п.;
* упаковка и распаковка ф.п., как с паролем, так и без него;
* получение любого файла прямо в чат-бот (соответствующий требо-ваниям для передачи через Telegram); получение/отправка любого файла на электронную почту (соответ-ствующий требованиям для передачи через электронную почту);
* создание и получение скриншота на удаленном устройстве;
* передачу команд для выполнения в терминале, как в ОС Windows (командная строка), так и в ОС Linux и получением ответа выполне-ния команды;
* удаленное редактирование личных настроек пользователя.

## Что необходимо подготовить перед сборкой?
1. Убедиться, что установлены или установить: JDK не ниже 17-ой версии и Apache Maven. 
2. Данные для нового зарегистрированного Telegram-бота: токен, наименование, id получателя сообщений во время эксплуатации и id администратора.
3. Данные базы данных (проверена работа только PostgreSQL): путь, логин и пароль.
4. Данные электронной почты: адреса серверов для входящей и исходящей почты, порты для них, логин и пароль
5. Данные - личные настройки пользователя: адреса персональной и дополнительной другой электронных почт, для отправки на них файлов, пароль упаковки/распаковки файлов и папок в файловой системе.
6. Прописать все необходимые пути:
   - путь для логов - в теге <property name="HOME_LOG" value="..\logs\dogo.log"/> файла logback.xml в папке resources;
   - путь для скриншотов - в переменной **screenshotDirPath** класса **Screenshoter** в пакете **service**;
   - путь для JSON-файла личных настроек пользователя - в константе **FILE_PATH** класса **UserConfig** пакета **config**. 
7. Заполнить данные пункта 5 в JSON-файл личных настроек пользователя.
8. Заполнить данные пункта 2-4 в файл конфигурации **application.properties**.

### Полный перечень настроек, который должен быть в файле **application.properties**
Менять только значения и убрать квадратные скобки после равно.
* spring.application.name=[наименование приложения - Dogobot]
* spring.main.headless=false
* telegrambot.name=botdogobot
* telegrambot.token=[токен, полученный для бота]
* telegrambot.authorId=[id автора программы]
* telegrambot.ownerId=[id владельца программы]
* spring.datasource.url=[jdbc:postgresql-путь к базе данных]
* spring.datasource.username=[username для входа в базу данных]
* spring.datasource.password=[пароль для входа в базу данных]
* spring.datasource.driver-class-name=org.postgresql.Driver
* spring.jpa.hibernate.ddl-auto=update
* spring.jpa.show-sql=true
* email.smtp.host=[сервер исходящей почты]
* email.smtp.port=[порт исходящей почты]
* email.imap.host=[сервер входящей почты]
* email.imap.port=[порт входящей почты]
* email.from=[почтовый адрес с которого будут отправляться письма]
* email.password=[пароль от почтового адреса]

## Для сборки проекта в jar-архив выполните команду (архив появится в корне проекта в папке **target**)

`mvn clean && mvn compile && mvn package`

## Для запуска jar-архива выполните команду

`java -jar [путь к Вашему jar-архиву]`

