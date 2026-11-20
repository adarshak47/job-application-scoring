package com.adarsh.job_application_scoring.application.question.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record BulkAliasRequest(

        @NotEmpty
        List<@NotBlank String> aliases
) {
}