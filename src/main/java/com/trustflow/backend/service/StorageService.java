package com.trustflow.backend.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class StorageService {

    private final Path baseDir;

    public StorageService() {
        String env = System.getenv("TF_STORAGE_DIR");
        if (env == null || env.isBlank()) env = "./storage";
        this.baseDir = Path.of(env).toAbsolutePath().normalize();
    }

    public Path getBaseDir() { return baseDir; }

    public Path ensureDir(String child) throws IOException {
        Path dir = baseDir.resolve(child).normalize();
        Files.createDirectories(dir);
        return dir;
    }


    public String randomName(String prefix, String ext) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
    }


    public Path write(String subdir, String filename, byte[] bytes) throws IOException {
        Path dir = ensureDir(subdir);
        Path p = dir.resolve(filename).normalize();
        Files.write(p, bytes);
        return p;
    }
}
