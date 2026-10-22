package com.adarsh.job_application_scoring.evaluation.dto.response;

public record RequirementAssessment(
        String requirement,
        RequirementType requirementType,
        RequirementStatus requirementStatus,
        String resumeEvidence,
        String explanation
) {
}
