package com.adarsh.job_application_scoring.candidate.service;

import com.adarsh.job_application_scoring.candidate.config.ResumeStorageProperties;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeStorageService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final ResumeStorageProperties properties;

    public StoredResume store(MultipartFile file) {
        validate(file);

        String originalFileName = StringUtils.cleanPath(
                file.getOriginalFilename()
        );

        String storedFileName = UUID.randomUUID() + ".pdf";

        Path directory = properties.getResumeDirectory()
                .toAbsolutePath()
                .normalize();

        try {
            byte[] content = file.getBytes();

            try (PDDocument document = Loader.loadPDF(content)) {
                if (document.getNumberOfPages() == 0) {
                    throw new IllegalArgumentException(
                            "Resume PDF must contain at least one page."
                    );
                }
            }

            Files.createDirectories(directory);

            Path destination = directory.resolve(storedFileName).normalize();

            if (!destination.startsWith(directory)) {
                throw new IllegalArgumentException("Invalid file path.");
            }

            Files.write(destination, content);

            return new StoredResume(originalFileName, storedFileName);

        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store resume.", exception);
        }
    }

    public void delete(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            return;
        }

        Path fileName = Path.of(storedFileName);

        if (fileName.getNameCount() != 1) {
            throw new IllegalArgumentException("Invalid stored file name.");
        }

        Path file = properties.getResumeDirectory()
                .toAbsolutePath()
                .normalize()
                .resolve(fileName)
                .normalize();

        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to delete old resume.", exception);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Resume file is required.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Resume must not exceed 10 MB.");
        }

        String filename = file.getOriginalFilename();

        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Resume must have a PDF extension.");
        }
    }

    public record StoredResume(
            String originalFileName,
            String storedFileName
    ) {}

    public Path resolvePath(String storedFileName) {

        if (storedFileName == null || storedFileName.isBlank()) {
            throw new IllegalArgumentException("Stored resume filename is required.");
        }

        Path fileName = Path.of(storedFileName);

        if (fileName.getNameCount() != 1 || fileName.isAbsolute()) {
            throw new IllegalArgumentException("Invalid stored resume filename.");
        }

        Path directory = properties.getResumeDirectory()
                .toAbsolutePath()
                .normalize();

        Path resolvedPath = directory.resolve(fileName).normalize();

        if (!resolvedPath.startsWith(directory)) {
            throw new IllegalArgumentException("Invalid resume file path.");
        }

        return resolvedPath;
    }
}