
package com.adarsh.job_application_scoring.application.question.dto;

import com.adarsh.job_application_scoring.application.question.model.ApplicationQuestionFieldIdentifierType;

public record ApplicationQuestionFieldIdentifier(
        ApplicationQuestionFieldIdentifierType identifierType,
        String fieldIdentifier
) {
}
