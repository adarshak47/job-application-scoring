package com.adarsh.job_application_scoring.application.browser.dto;

import lombok.Builder;

@Builder
public record WorkdayApplicationStartResult(
        WorkdayApplyResult applyResult,
        WorkdayApplyOptionsResult applyOptionsResult,
        WorkdaySignInResult signInResult,
        WorkdayLoginResult loginResult,
        WorkdayMyInformationStepResult myInformationStepResult,
        WorkdayMyExperienceStepResult myExperienceStepResult,
        WorkdayApplicationQuestionsStepResult applicationQuestionsStepResult,
        WorkdayVoluntaryDisclosuresStepResult voluntaryDisclosuresStepResult,
        String finalUrl,
        WorkdayApplicationStepInspectionResult nextStepInspectionResult
) {}