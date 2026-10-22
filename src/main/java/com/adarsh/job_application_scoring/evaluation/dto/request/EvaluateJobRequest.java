package com.adarsh.job_application_scoring.evaluation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EvaluateJobRequest(
        @NotBlank(message = "Resume text cannot be blank")
        @Size(max = 50000, message = "Resume Text must not exceed 50000 characters")
        String resumeText,

        @NotBlank(message = "Job Description cannot be blank.")
        @Size(max = 30000, message = "Job description cannot be more that 30000 characters")
        String jobDescription
) {}
