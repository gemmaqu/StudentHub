package com.studenthub.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/** Stores uploads on local disk under generated names, so client filenames never reach the filesystem. */
@Service
public class FileStorage {

    private final Path root;

    public FileStorage(@Value("${studenthub.storage.dir}") Path root) throws IOException {
        this.root = Files.createDirectories(root);
    }

    public String store(MultipartFile file) {
        String key = UUID.randomUUID().toString();
        try {
            file.transferTo(root.resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store upload", e);
        }
        return key;
    }

    public Optional<Resource> load(String key) {
        Path path = root.resolve(key);
        return Files.isRegularFile(path) ? Optional.of(new FileSystemResource(path)) : Optional.empty();
    }

    public void delete(String key) {
        try {
            Files.deleteIfExists(root.resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete upload", e);
        }
    }
}
