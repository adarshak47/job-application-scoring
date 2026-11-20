package com.adarsh.job_application_scoring.application.browser.dto;

import com.adarsh.job_application_scoring.application.browser.model.WorkdayLoginVerificationStatus;

import java.util.List;

public record WorkdayLoginVerificationResult(
        WorkdayLoginVerificationStatus status,
        String resultingUrl,
        List<String> detectedSignals
) {
}