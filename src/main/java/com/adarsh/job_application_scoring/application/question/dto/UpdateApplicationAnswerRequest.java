package com.adarsh.job_application_scoring.application.question.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public record UpdateApplicationAnswerRequest(

        @NotNull
        JsonNode answerValue
) {
}