package com.adarsh.job_application_scoring.ai.dto;

import com.adarsh.job_application_scoring.evaluation.dto.response.RequirementAssessment;

import java.util.List;

public record GeminiAssessmentPayload (
        List<RequirementAssessment> requirementAssessments,
        List<String> strengths,
        List<String> risks
) {
}
