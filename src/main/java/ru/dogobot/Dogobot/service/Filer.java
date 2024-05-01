package ru.dogobot.Dogobot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.dogobot.Dogobot.exception.FilerException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
public class Filer {

    /**
     * Переименовывает папку или файл
     * @param source исходный файл или папка для переименования
     * @param newName новое имя
     * @return новый файл или папка с новым именем
     * @throws FilerException если возникли исключения
     */
    public File renameFileDir(File source, String newName) throws FilerException {
        if (!source.exists()) {
            throw new FilerException("Ссылка на файл или папку для переименования не корректна: '" + source.getName() + "'");
        }
        if (newName.isEmpty()) {
            throw new FilerException("Новое имя пустое для: '" + source.getName() + "'");
        }
        if (source.getName().equals(newName)) {
            throw new FilerException("Старое и новое имена совпадают: '" + source.getName() + "'");
        }

        File fileWithNewName = new File(source.getParent(), newName);
        if (!source.renameTo(fileWithNewName)) {
            throw new FilerException("Не удалось переименовать папку или файл: '" + source.getName() + "' в: '" + newName + "'");
        }
        return fileWithNewName;
    }

    /**
     * Перемещает папку или файл
     * @param source исходный файл или папка для перемещения
     * @param destinationPath адрес к новому пути
     * @return новый файл или папка с новым путем
     * @throws FilerException если возникли исключения
     */
    public File moveFileDir(File source, String destinationPath) throws FilerException {
        if (!source.exists()) {
            throw new FilerException("Ссылка на файл или папку для перемещения не корректна: '" + source.getName() + "'");
        }
        if (destinationPath.isEmpty()) {
            throw new FilerException("Адрес к новому пути пустой для: '" + source.getName() + "'");
        }
        if (source.getName().equals(destinationPath)) {
            throw new FilerException("Старый и новый пути совпадают: '" + source.getAbsolutePath() + "'");
        }

        File fileWithNewPath = new File(destinationPath);
        if (!source.renameTo(fileWithNewPath)) {
            throw new FilerException("Не удалось переместить папку или файл: '" + source.getAbsolutePath() + "' в: '" + destinationPath + "'");
        }
        return fileWithNewPath;
    }

    /**
     * Копирует папку или файл
     * @param source путь к исходному файлу или папке
     * @param destination путь к новому месту
     * @return новый файл или папка
     * @throws IOException если возникли исключения
     * @throws FilerException если возникли исключения
     */
    public File copyFileDir(Path source, Path destination) throws IOException, FilerException {
        if (source == null || !source.toFile().exists() || destination == null) {
            throw new FilerException("Ссылки на исходный файл/папку или папку для копирования не корректны");
        }

        if (!Files.isDirectory(source)) {
            return Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
                    .toFile();
        }

        if (!Files.exists(destination)) {
            Files.createDirectories(destination);
        }
        Files.walk(source)
                .forEach(sourcePath -> {
                    Path destPath = destination.resolve(source.relativize(sourcePath));
                    try {
                        if (Files.isDirectory(sourcePath)) {
                            if (!Files.exists(destPath)) {
                                Files.createDirectory(destPath);
                            }
                        } else {
                            Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Копирование аварийно остановлено. Причина - не удалось скопировать '" + sourcePath + "' в '" + destPath + "' " + System.lineSeparator() + e.getMessage());
                    }
                });
        return destination.toFile();
    }

    /**
     * Рекурсивно удаляет папку или файл
     * @param fileOrDir исходный файл или папка
     */
    public void deleteFileDir(File fileOrDir) {
        // Recursive deletion for directories
        if (fileOrDir.isDirectory()) {
            File[] contents = fileOrDir.listFiles();
            if (contents != null) {
                for (File f : contents) {
                    deleteFileDir(f);
                }
            }
        }
        // Delete the file or empty directory
        fileOrDir.delete();
    }
}