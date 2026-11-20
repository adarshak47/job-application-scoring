
package com.adarsh.job_application_scoring.application.browser.dto;

import java.util.List;

public record WorkdayApplicationStepInspectionResult(
        boolean applicationStepDetected,
        String stepAutomationId,
        String resultingUrl,
        List<String> sectionHeadings,
        List<WorkdayFormFieldInspection> fields,
        List<String> visibleButtons,
        List<String> detectedSignals
) {}
