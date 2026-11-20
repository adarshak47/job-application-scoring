package com.adarsh.job_application_scoring.application.browser.dto;

public record WorkdayVoluntaryDisclosuresStepResult(
        boolean successful,
        String status,
        String selectedGender,
        boolean genderSelectionVerified,
        boolean privacyAcknowledgementVerified,
        WorkdayApplicationStepInspectionResult formInspectionResult,
        WorkdaySaveAndContinueResult saveAndContinueResult
) {
}