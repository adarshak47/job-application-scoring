package com.adarsh.job_application_scoring.application.browser.dto;

import com.adarsh.job_application_scoring.application.browser.model.WorkdayApplyStatus;

import java.util.List;

public record WorkdayApplyResult(
        WorkdayApplyStatus status,
        List<String> detectedSignals
) {
}