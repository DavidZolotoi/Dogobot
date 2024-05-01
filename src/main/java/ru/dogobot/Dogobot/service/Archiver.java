package ru.dogobot.Dogobot.service;

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
     * Упаковка файла или папки в архив zip без пароля
     *
     * @param sourceForAdd путь к исходному файлу или папке
     * @return путь к сохраненному архиву
     * @throws ZipException если возникли исключения при упаковке
     */
    public String zipFolderWithoutPassword(String sourceForAdd) throws ZipException {
        File fileOrDirForAdd = new File(sourceForAdd);
        if (!fileOrDirForAdd.exists()
                || (!fileOrDirForAdd.isFile() && !fileOrDirForAdd.isDirectory())
        ) {
            throw new ZipException("Файл или папка не найдены и не упакованы (метод без пароля): " + sourceForAdd);
        }

        ZipParameters parameters = new ZipParameters();
        parameters.setCompressionMethod(CompressionMethod.DEFLATE);
        parameters.setCompressionLevel(CompressionLevel.NORMAL);
        parameters.setEncryptFiles(false);
        parameters.setEncryptionMethod(EncryptionMethod.NONE);

        String zipFilePath = "%s_%s.zip".formatted(
                sourceForAdd,
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HHmmss"))
        );
        ZipFile zipFile = new ZipFile(zipFilePath);
        if (fileOrDirForAdd.isDirectory()) {
            zipFile.addFolder(fileOrDirForAdd, parameters);
            return zipFile.getFile().getAbsolutePath();
        }
        if (fileOrDirForAdd.isFile()) {
            zipFile.addFile(fileOrDirForAdd, parameters);
            return zipFile.getFile().getAbsolutePath();
        }
        throw new ZipException("Файл или папка не найдены и не упакованы (метод без пароля): " + sourceForAdd);
    }

    /**
     * Упаковка файла или папки в архив zip с паролем
     *
     * @param sourceForAdd путь к исходному файлу или папке
     * @param password     пароль для архива
     * @return путь к сохраненному архиву
     * @throws ZipException если возникли исключения при упаковке
     */
    public String zipFolderWithPassword(String sourceForAdd, String password) throws ZipException {
        File fileOrDirForAdd = new File(sourceForAdd);
        if (!fileOrDirForAdd.exists()
                || (!fileOrDirForAdd.isFile() && !fileOrDirForAdd.isDirectory())
        ) {
            throw new ZipException("Файл или папка не найдены и не упакованы (метод без пароля): " + sourceForAdd);
        }

        ZipParameters parameters = new ZipParameters();
        parameters.setCompressionMethod(CompressionMethod.DEFLATE);
        parameters.setCompressionLevel(CompressionLevel.NORMAL);
        parameters.setEncryptFiles(false);
        parameters.setEncryptionMethod(EncryptionMethod.NONE);

        String zipFilePath = "%s_%s.zip".formatted(
                sourceForAdd,
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HHmmss"))
        );
        ZipFile zipFile = new ZipFile(zipFilePath);
        zipFile.setPassword(password.toCharArray());
        if (fileOrDirForAdd.isDirectory()) {
            zipFile.addFolder(fileOrDirForAdd, parameters);
            return zipFile.getFile().getAbsolutePath();
        }
        if (fileOrDirForAdd.isFile()) {
            zipFile.addFile(fileOrDirForAdd, parameters);
            return zipFile.getFile().getAbsolutePath();
        }
        throw new ZipException("Файл или папка не найдены и не упакованы (метод без пароля): " + sourceForAdd);
    }

    /**
     * Распаковка архива zip без пароля
     * @param zipFilePath путь к исходному архиву
     * @return путь к распакованному архиву
     * @throws ZipException если возникли исключения при распаковке
     */
    public String unzipFileWithoutPassword(String zipFilePath) throws ZipException {
        if (zipFilePath == null || !new File(zipFilePath).exists()) {
            throw new ZipException("Архив не найден и не распакован (метод без пароля): " + zipFilePath);
        }

        ZipFile zipFile = new ZipFile(zipFilePath);
        if (zipFile.isEncrypted()) {
            throw new ZipException("Архив не распакован (метод без пароля), он оказался с паролем.");
        }

        String zipFileParentPath = new File(zipFilePath).getParent();
        zipFile.extractAll(zipFileParentPath);

        return zipFileParentPath;
    }

    /**
     * Распаковка архива zip с паролем
     * @param zipFilePath путь к исходному архиву
     * @param password пароль для архива
     * @return путь к распакованному архиву
     * @throws ZipException если возникли исключения при распаковке
     */
    public String unzipFileWithPassword(String zipFilePath, String password) throws ZipException {
        if (zipFilePath == null || !new File(zipFilePath).exists()) {
            throw new ZipException("Архив не найден и не распакован (метод без пароля): " + zipFilePath);
        }

        ZipFile zipFile = new ZipFile(zipFilePath);
        if (zipFile.isEncrypted()) {
            zipFile.setPassword(password.toCharArray());
        }

        String zipFileParentPath = new File(zipFilePath).getParent();
        zipFile.extractAll(zipFileParentPath);

        return zipFileParentPath;
    }

}