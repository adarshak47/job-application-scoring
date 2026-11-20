package com.adarsh.job_application_scoring.application.browser.dto;

import com.adarsh.job_application_scoring.application.browser.model.WorkdayApplyOptionsStatus;

import java.util.List;

public record WorkdayApplyOptionsResult(
        WorkdayApplyOptionsStatus status,
        List<String> detectedSignals
) {
}