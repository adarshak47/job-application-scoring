package com.adarsh.job_application_scoring.application.browser.dto;

public record WorkdayMyExperienceStepResult(
        WorkdayWorkExperienceSectionResult workExperienceResult,
        WorkdayEducationStepResult educationResult,
        WorkdayResumeUploadStepResult resumeUploadResult,
        WorkdayApplicationStepInspectionResult formInspectionResult,
        WorkdayApplicationReadinessResult readinessResult,
        WorkdaySaveAndContinueResult saveAndContinueResult
) {
}