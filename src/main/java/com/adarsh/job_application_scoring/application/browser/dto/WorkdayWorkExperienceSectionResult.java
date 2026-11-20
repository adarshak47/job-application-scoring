package com.adarsh.job_application_scoring.application.browser.dto;

import java.util.List;

public record WorkdayWorkExperienceSectionResult(
        boolean successful,
        String status,
        int expectedRecordCount,
        int processedRecordCount,
        List<String> missingDataFields,
        List<WorkExperienceFillResult> workExperienceResults,
        List<String> detectedSignals
) {

    public record WorkExperienceFillResult(
            int recordNumber,
            boolean successful,
            String status,
            List<String> filledFields,
            List<String> missingFields,
            String errorMessage
    ) {
    }
}