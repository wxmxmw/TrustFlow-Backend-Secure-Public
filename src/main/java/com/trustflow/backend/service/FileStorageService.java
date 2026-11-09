package com.trustflow.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

@Service
public class FileStorageService {

    @Value("${app.upload-dir:/data/uploads/}")
    private String uploadDir;
    @Value("${app.image.output-dir:./generate-image/}")
    private String imageOutputDir;

    public FileStorageResult storeFile(MultipartFile file) throws IOException {

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }


        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String originalFilename = file.getOriginalFilename();
        String fileName = timestamp + "_" + originalFilename;


        Path targetLocation = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);


        String fileHash = calculateFileHash(targetLocation);

        return new FileStorageResult(targetLocation.toString(), fileHash, file.getSize(), file.getContentType(),
                originalFilename);
    }

    public void deleteFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            Files.delete(path);
        }
    }

    public FileStorageResult storeBytes(byte[] bytes, String originalFilename, String contentType) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = timestamp + "_" + originalFilename;
        Path targetLocation = uploadPath.resolve(fileName);
        Files.write(targetLocation, bytes,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                java.nio.file.StandardOpenOption.WRITE);

        String fileHash = calculateFileHash(targetLocation);
        return new FileStorageResult(targetLocation.toString(), fileHash, (long) bytes.length, contentType,
                originalFilename);
    }

    public FileStorageResult storeImageBytes(byte[] bytes, String originalFilename, String contentType)
            throws IOException {
        Path outPath = Paths.get(imageOutputDir);
        if (!Files.exists(outPath)) {
            Files.createDirectories(outPath);
        }
        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = timestamp + "_" + originalFilename;
        Path targetLocation = outPath.resolve(fileName);
        Files.write(targetLocation, bytes,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                java.nio.file.StandardOpenOption.WRITE);
        String fileHash = calculateFileHash(targetLocation);
        return new FileStorageResult(targetLocation.toString(), fileHash, (long) bytes.length, contentType,
                originalFilename);
    }

    private String calculateFileHash(Path filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hashBytes = digest.digest(fileBytes);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public static class FileStorageResult {
        private final String filePath;
        private final String fileHash;
        private final Long fileSize;
        private final String mimeType;
        private final String fileName;

        public FileStorageResult(String filePath, String fileHash, Long fileSize, String mimeType, String fileName) {
            this.filePath = filePath;
            this.fileHash = fileHash;
            this.fileSize = fileSize;
            this.mimeType = mimeType;
            this.fileName = fileName;
        }

        // Getters
        public String getFilePath() {
            return filePath;
        }

        public String getFileHash() {
            return fileHash;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getFileName() {
            return fileName;
        }
    }
}
