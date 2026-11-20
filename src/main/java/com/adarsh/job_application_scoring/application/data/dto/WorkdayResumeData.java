package com.adarsh.job_application_scoring.application.data.dto;

import java.nio.file.Path;

public record WorkdayResumeData(
        String fileName,
        Path filePath
) {
}