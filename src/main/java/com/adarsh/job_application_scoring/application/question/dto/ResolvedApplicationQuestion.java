package com.adarsh.job_application_scoring.application.question.dto;

import com.adarsh.job_application_scoring.application.question.model.AnswerType;
import com.fasterxml.jackson.databind.JsonNode;

public record ResolvedApplicationQuestion(
        String originalQuestion,
        String questionKey,
        AnswerType answerType,
        JsonNode answerValue
) {
}