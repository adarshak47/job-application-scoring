package com.adarsh.job_application_scoring.application.browser.dto;

public record WorkdayBrowserResponse(
        String requestedUrl,
        String finalUrl,
        String pageTitle,
        Integer httpStatus
) {
}