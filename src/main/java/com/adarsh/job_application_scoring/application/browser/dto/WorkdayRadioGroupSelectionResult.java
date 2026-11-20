package com.adarsh.job_application_scoring.application.browser.dto;

public record WorkdayRadioGroupSelectionResult(
        String label,
        boolean required,
        boolean selected,
        String selectedValue,
        String selectedLabel,
        String status,
        String message
) {
}