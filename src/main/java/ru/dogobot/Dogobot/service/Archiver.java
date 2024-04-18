package ru.dogobot.Dogobot.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
public class Archiver {

    /**
     * Сжатие папки в архив zip без пароля
     * @param sourceFolder путь исходной папке
     */
    public void zipFolderWithoutPassword(String sourceFolder) {
        try {
            String zipFilePath = "%s_%s.zip".formatted(
                    sourceFolder,
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HHmmss"))
            );

            File folderToAdd = new File(sourceFolder);
            ZipParameters parameters = new ZipParameters();
            parameters.setCompressionMethod(CompressionMethod.DEFLATE);
            parameters.setCompressionLevel(CompressionLevel.NORMAL);
            parameters.setEncryptFiles(false);
            parameters.setEncryptionMethod(EncryptionMethod.NONE);

            ZipFile zipFile = new ZipFile(zipFilePath);
            zipFile.addFolder(folderToAdd, parameters);

            log.info("Папка успешно сжата в архив без установки пароля.");
        } catch (ZipException e) {
            log.error("Не удалось сжать папку (метод без пароля): " + e.getMessage());
        }
    }

    /**
     * Сжатие папки в архив zip с паролем
     * @param sourceFolder путь исходной папке
     * @param password пароль для установки на архив
     */
    public void zipFolderWithPassword(String sourceFolder, String password) {
        try {
            String zipFilePath = "%s_%s.zip".formatted(
                    sourceFolder,
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HHmmss"))
            );

            File folderToAdd = new File(sourceFolder);
            ZipParameters parameters = new ZipParameters();
            parameters.setCompressionMethod(CompressionMethod.DEFLATE);
            parameters.setCompressionLevel(CompressionLevel.NORMAL);
            parameters.setEncryptFiles(true);
            parameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);

            ZipFile zipFile = new ZipFile(zipFilePath);
            zipFile.setPassword(password.toCharArray());
            zipFile.addFolder(folderToAdd, parameters);

            log.info("Папка успешно сжата в архив с установкой пароля.");
        } catch (ZipException e) {
            log.error("Не удалось сжать папку (метод с паролем): " + e.getMessage());
        }
    }

    /**
     * Распаковка архива zip без пароля
     * @param zipFilePath путь к исходному архиву
     */
    public void unzipFileWithoutPassword(String zipFilePath) {
        //todo добавить проверку на существование папки
        try {
            String zipFileParentPath = new File(zipFilePath).getParent();
            ZipFile zipFile = new ZipFile(zipFilePath);
            if (zipFile.isEncrypted()) {
                //todo выкинуть исключение, потому что этот архив должен был быть без пароля
                log.error("Архив оказался с паролем.");
                return;
            }
            zipFile.extractAll(zipFileParentPath);

            log.info("Архив успешно распакован без пароля.");
        } catch (ZipException e) {
            log.error("Не удалось распаковать архив (метод без пароля): " + e.getMessage());
        }
    }
    /**
     * Распаковка архива zip с паролем
     * @param zipFilePath путь к исходному архиву
     * @param password пароль для архива для распаковки
     */
    public void unzipFileWithPassword(String zipFilePath, String password) {
        //todo добавить проверку на существование папки
        try {
            String zipFileParentPath = new File(zipFilePath).getParent();
            ZipFile zipFile = new ZipFile(zipFilePath);
            if (zipFile.isEncrypted()) {
                zipFile.setPassword(password.toCharArray());
            }
            zipFile.extractAll(zipFileParentPath);

            log.info("Архив успешно распакован с паролем.");
        } catch (ZipException e) {
            log.error("Не удалось распаковать архив (метод с паролем): " + e.getMessage());
        }
    }

}