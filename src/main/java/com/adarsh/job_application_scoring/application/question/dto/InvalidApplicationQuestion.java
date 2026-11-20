package com.adarsh.job_application_scoring.application.question.dto;

public record InvalidApplicationQuestion(
        String originalQuestion,
        String questionKey,
        String reason
) {
}