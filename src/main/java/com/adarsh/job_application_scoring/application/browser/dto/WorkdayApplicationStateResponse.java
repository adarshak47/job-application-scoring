package com.adarsh.job_application_scoring.application.browser.dto;

import java.util.List;

public record WorkdayApplicationStateResponse(
        String requestedUrl,
        String finalUrl,
        String jobTitle,
        Integer httpStatus,
        WorkdayApplicationState state,
        List<String> detectedSignals
) {
}