package com.adarsh.job_application_scoring.application.browser.dto;

import jakarta.validation.constraints.NotBlank;

public record WorkdayApplyRequest(
        @NotBlank String applicationUrl
) {
}