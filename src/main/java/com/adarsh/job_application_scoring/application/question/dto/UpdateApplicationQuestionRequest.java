package com.adarsh.job_application_scoring.application.question.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateApplicationQuestionRequest(

        @NotBlank
        String canonicalQuestion,

        @NotBlank
        String category
) {
}