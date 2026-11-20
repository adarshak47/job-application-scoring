package com.adarsh.job_application_scoring.application.browser.dto;

import java.util.List;

public record WorkdaySourceSelectionResult(
        boolean selected,
        String selectedOption,
        String status,
        List<String> detectedOptions
) {
}