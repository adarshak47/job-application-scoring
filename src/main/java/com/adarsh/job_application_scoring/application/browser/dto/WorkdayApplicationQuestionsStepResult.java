package com.adarsh.job_application_scoring.application.browser.dto;

import com.adarsh.job_application_scoring.application.question.dto.ApplicationQuestionResolutionResponse;

import java.util.List;

public record WorkdayApplicationQuestionsStepResult(
        boolean successful,
        String status,
        ApplicationQuestionResolutionResponse resolutionResult,
        List<WorkdayApplicationQuestionAnswerResult> answerResults,
        WorkdayApplicationStepInspectionResult formInspectionResult,
        WorkdaySaveAndContinueResult saveAndContinueResult
) {
}