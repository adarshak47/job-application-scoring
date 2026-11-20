
package com.adarsh.job_application_scoring.application.browser.dto;

import java.util.List;

public record WorkdayEducationStepResult(
        boolean successful,
        String status,
        int expectedRecordCount,
        int processedRecordCount,
        List<String> missingDataFields,
        List<EducationFillResult> educationResults,
        List<String> detectedSignals
) {
    public record EducationFillResult(
            int recordNumber,
            boolean successful,
            String status,
            List<String> filledFields,
            List<String> missingFields,
            String errorMessage
    ) {}
}
