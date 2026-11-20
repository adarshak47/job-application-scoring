
package com.adarsh.job_application_scoring.application.browser.dto;

import java.util.List;

public record WorkdayMyInformationFillResult(
        boolean successful,
        String status,
        List<String> filledFields,
        List<String> missingFields,
        List<String> detectedSignals
) {
}
