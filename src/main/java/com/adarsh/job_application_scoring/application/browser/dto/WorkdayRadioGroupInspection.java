
package com.adarsh.job_application_scoring.application.browser.dto;

import com.adarsh.job_application_scoring.application.question.dto.ApplicationQuestionFieldIdentifier;

import java.util.List;

public record WorkdayRadioGroupInspection(
        String label,
        boolean required,
        List<ApplicationQuestionFieldIdentifier> identifiers,
        List<WorkdayRadioOptionInspection> options
) {
}
