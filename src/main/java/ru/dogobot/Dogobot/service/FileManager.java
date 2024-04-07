package ru.dogobot.Dogobot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.dogobot.Dogobot.model.FileDir;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class FileManager {
    @Autowired
    private FileDir fileDir;

    /**
     * Заполняет и получает объект FileDir (элемент файловой системы)
     * @param inputPath путь к элементу файловой системы
     * @return объект FileDir
     */
    public FileDir getFileDir(String inputPath) {

        fileDir.setFdObject(
                new File(inputPath)
        );

        fileDir.setFdArray(
                getArrayFromPath()
        );

        //todo !!!!!!!!!!!!!
        // тут есть прогон. В этот прогон надо добавить функционал -
        // заполнение словаря колбэками Inline-кнопок. Нужно вводить ID!
        // Надо сеттер переделать в другой универсальный метод с прогоном.
        // Надо добавить кнопку для возврата в предыдущую папку.
        fileDir.setFdInlineKeyboardNames(
                getInlineKeyboardNames()
        );

        fileDir.setFdPath(
                fileDir.getFdObject().getAbsolutePath()
        );

        fileDir.setFdType(
                (fileDir.getFdObject().isFile())?
                        FileDir.FDType.FILE :
                        FileDir.FDType.DIR
        );

        fileDir.setFdNameOriginal(
                fileDir.getFdObject().getName()
        );

        fileDir.setFdNameInline(
                getFormatedTextForInlineButton(fileDir.getFdNameOriginal())
        );

        fileDir.setFdDate(
                fileDir.getFdObject().lastModified()
        );

        fileDir.setFdSize(
                fileDir.getFdObject().length()
        );

        return fileDir;
    }

    /**
     * Проверяет и корректирует, в случае необходимости, названия Inline-кнопок в соответствии с требованиями.
     * Корректировка происходит путём удаления неразрешенных символов и уменьшения длины строки до максимально допустимой.
     * @param input название кнопки для проверки и коррекции
     * @return проверенное и корректное название
     */
    public String getFormatedTextForInlineButton(String input){
        // Оставляем только разрешенные символы
        //todo для поддержки других языков надо либо добавлять сюда,
        // либо стратегия "удаляем неразрешенные символы"
        String sanitizedInput = input.replaceAll("[^a-zA-Zа-яА-Я0-9 .,:;`~'\"!?@#№$%^&*-_+=|<>(){}\\[\\]]", "");
        int maxLength = 30; //еще 2 оставляю для квадратных скобок папок []
        if (sanitizedInput.length() <= maxLength)
            return sanitizedInput;

        // Уменьшаем длину строки до максимальной
        int charactersToRemove = sanitizedInput.length() - maxLength + 2;
        int start = sanitizedInput.length() / 2 - charactersToRemove / 2;
        return sanitizedInput.substring(0, start)
                + ".."
                + sanitizedInput.substring(start + charactersToRemove);
    }

    /**
     * Сортирует список элементов, поднимая папки перед файлами.
     * В случае если текущий элемент - папка, то добавляет ткущую папку в начало массива с содержимым папки.
     * @return отсортированный массив элементов файловой системы, включая текущий.
     */
    private File[] getArrayFromPath() {
        File fileByNextPath = fileDir.getFdObject();
        File[] dirsAndFilesWithoutCurrent = fileByNextPath.listFiles();
        int arrayLenght = 1;
        if (dirsAndFilesWithoutCurrent != null) {
            arrayLenght += dirsAndFilesWithoutCurrent.length;
            Arrays.sort(dirsAndFilesWithoutCurrent, (file1, file2) -> {
                if (file1.isDirectory() && !file2.isDirectory()) {
                    return -1; // Поместить папку перед файлом
                } else if (!file1.isDirectory() && file2.isDirectory()) {
                    return 1; // Поместить файл после папки
                } else {
                    return file1.getName().compareTo(file2.getName()); // Оставить порядок для файлов и папок неизменным
                }
            });
        }
        File[] dirsAndFilesWithCurrentDir = new File[arrayLenght];
        dirsAndFilesWithCurrentDir[0] = fileByNextPath;
        if (dirsAndFilesWithoutCurrent != null) {
            System.arraycopy(
                    dirsAndFilesWithoutCurrent, 0,
                    dirsAndFilesWithCurrentDir, 1,
                    fileByNextPath.listFiles().length
            );
        }
        return dirsAndFilesWithCurrentDir;
    }

    /**
     * Создает таблицу (коллекция коллекций) названий для Inline-клавиатуры
     * @return таблица названий для кнопок Inline-клавиатуры
     */
    private List<List<String>> getInlineKeyboardNames() {
        Long fileCount = 0L, dirCount = 0L;
        String fileDirId = null;
        List<List<String>> inlineKeyboardNames = new ArrayList<>();
        for(File file : fileDir.getFdArray()) {
            if (file.isDirectory()) {
                fileDirId = "d%d".formatted(dirCount);
                dirCount++;
            }
            if (file.isFile()) {
                fileDirId = "f%d".formatted(fileCount);
                fileCount++;
            }

            //Название кнопки, обработанное под требования telegram-bot-api
            String fileDirName = getFormatedTextForInlineButton(file.getName());
            if (file.isDirectory()) {
                fileDirName = "[" + fileDirName + "]";
            }
            List<String> row = new ArrayList<>();
            //row.add(fileDirId);
            row.add(fileDirName);
            inlineKeyboardNames.add(row);
        }
        return inlineKeyboardNames;
    }
}
