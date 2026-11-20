package com.adarsh.job_application_scoring.application.question.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkApplicationQuestionRequest(

        @NotEmpty
        List<@Valid ApplicationQuestionRequest> questions
) {
}