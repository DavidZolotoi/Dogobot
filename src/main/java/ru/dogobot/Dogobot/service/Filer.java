package ru.dogobot.Dogobot.service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
public class Filer {

    public boolean renameFileDir(File fileDir, String newName) {
        return fileDir.renameTo(new File(fileDir.getParent(), newName));
    }

    public boolean moveFileDir(File source, String destinationPath) {
        return source.renameTo(new File(destinationPath));
    }

    public void copyFileDir(Path source, Path destination) throws IOException {
        if (Files.isDirectory(source)) {
            if (!Files.exists(destination)) {
                Files.createDirectories(destination);
            }
            Files.walk(source)
                    .forEach(sourcePath -> {
                        try {
                            Path destPath = destination.resolve(source.relativize(sourcePath));
                            if (Files.isDirectory(sourcePath)) {
                                if (!Files.exists(destPath)) {
                                    Files.createDirectory(destPath);
                                }
                            } else {
                                Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } else {
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

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