package com.adarsh.job_application_scoring.application.browser.dto;

import jakarta.validation.constraints.NotBlank;

public record WorkdayBrowserRequest(
        @NotBlank(message = "Workday application URL is required")
        String applicationUrl
) {
}