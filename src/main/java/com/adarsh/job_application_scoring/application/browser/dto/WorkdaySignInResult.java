package com.adarsh.job_application_scoring.application.browser.dto;

import com.adarsh.job_application_scoring.application.browser.model.WorkdaySignInStatus;

import java.util.List;

public record WorkdaySignInResult(
        WorkdaySignInStatus status,
        List<String> detectedSignals
) {
}