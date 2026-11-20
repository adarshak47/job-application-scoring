
package com.adarsh.job_application_scoring.application.browser.dto;

import java.util.List;

public record WorkdayResumeUploadStepResult(
        boolean successful,
        String status,
        String fileName,
        long fileSizeBytes,
        List<String> detectedSignals
) {
}
