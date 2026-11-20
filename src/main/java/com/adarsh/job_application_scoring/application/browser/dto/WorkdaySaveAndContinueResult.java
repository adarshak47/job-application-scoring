
package com.adarsh.job_application_scoring.application.browser.dto;

import java.util.List;

public record WorkdaySaveAndContinueResult(
        boolean clicked,
        boolean transitionDetected,
        String status,
        String previousUrl,
        String resultingUrl,
        List<String> detectedSignals
) {}
