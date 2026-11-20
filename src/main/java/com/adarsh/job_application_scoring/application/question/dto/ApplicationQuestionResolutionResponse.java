package com.adarsh.job_application_scoring.application.question.dto;

import java.util.List;

public record ApplicationQuestionResolutionResponse(
        boolean canProceed,
        List<ResolvedApplicationQuestion> resolvedQuestions,
        List<String> unknownQuestions,
        List<InvalidApplicationQuestion> invalidQuestions
) {
}