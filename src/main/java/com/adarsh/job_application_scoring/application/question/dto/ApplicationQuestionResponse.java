package com.adarsh.job_application_scoring.application.question.dto;

import com.adarsh.job_application_scoring.application.question.model.AnswerType;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;

public record ApplicationQuestionResponse(

        Long id,
        String questionKey,
        String canonicalQuestion,
        String category,
        AnswerType answerType,
        JsonNode answerValue,
        List<String> aliases,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}