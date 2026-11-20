package com.adarsh.job_application_scoring.application.browser.dto;

import java.util.List;

public record WorkdayLoginResult(
        WorkdayLoginStatus status,
        List<String> detectedSignals
) {
}