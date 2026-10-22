package com.adarsh.job_application_scoring.evaluation.dto.response;

import java.util.List;

public record JobEvaluationResponse(
        int matchScore,
        EvaluationVerdict evaluationVerdict,
        List<RequirementAssessment> requirementAssessmens,
        List<String> strengths,
        List<String> risks,
        String recommendation
) {
}
