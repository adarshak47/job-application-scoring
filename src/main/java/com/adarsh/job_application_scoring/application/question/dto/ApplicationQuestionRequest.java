package com.adarsh.job_application_scoring.application.question.dto;

import com.adarsh.job_application_scoring.application.question.model.AnswerType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ApplicationQuestionRequest(

        @NotBlank
        String questionKey,

        @NotBlank
        String canonicalQuestion,

        @NotBlank
        String category,

        @NotNull
        AnswerType answerType,

        @NotNull
        JsonNode answerValue,

        List<@NotBlank String> aliases
) {
}