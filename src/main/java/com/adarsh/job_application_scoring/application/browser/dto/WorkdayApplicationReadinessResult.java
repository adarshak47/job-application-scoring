
package com.adarsh.job_application_scoring.application.browser.dto;

import java.util.List;

public record WorkdayApplicationReadinessResult(
        String status,
        boolean candidateDataReady,
        boolean formReady,
        boolean readyToProceed,
        List<String> availableRequiredFields,
        List<String> missingCandidateData,
        List<String> unfilledRequiredFields,
        List<String> blockingReasons
) {
}
