
package com.adarsh.job_application_scoring.application.question.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record ApplicationQuestionFieldResolutionResult(
        boolean resolved,
        String questionKey,
        String canonicalQuestion,
        String answerType,
        JsonNode answerValue,
        String status,
        String message
) {
}
