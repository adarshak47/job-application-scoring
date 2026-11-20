package com.adarsh.job_application_scoring.application.browser.dto;

import com.adarsh.job_application_scoring.application.question.dto.ApplicationQuestionFieldResolutionResult;

public record WorkdayRadioGroupResolution(
        String label,
        boolean required,
        ApplicationQuestionFieldResolutionResult resolution
) {
}