package com.adarsh.job_application_scoring.application.browser.dto;

public record WorkdayApplicationQuestionAnswerResult(
        String question,
        String questionKey,
        String selectedOption,
        boolean successful,
        String status
) {
}