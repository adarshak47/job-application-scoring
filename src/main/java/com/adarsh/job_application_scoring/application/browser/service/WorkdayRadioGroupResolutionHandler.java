package com.adarsh.job_application_scoring.application.browser.service;

import com.adarsh.job_application_scoring.application.browser.dto.WorkdayRadioGroupInspection;
import com.adarsh.job_application_scoring.application.browser.dto.WorkdayRadioGroupResolution;
import com.adarsh.job_application_scoring.application.question.dto.ApplicationQuestionFieldResolutionResult;
import com.adarsh.job_application_scoring.application.question.service.ApplicationQuestionFieldResolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkdayRadioGroupResolutionHandler {

    private final ApplicationQuestionFieldResolutionService fieldResolutionService;

    public List<WorkdayRadioGroupResolution> resolve(
            List<WorkdayRadioGroupInspection> radioGroups
    ) {

        if (radioGroups == null || radioGroups.isEmpty()) {
            return List.of();
        }

        return radioGroups.stream()
                .map(this::resolveGroup)
                .toList();
    }

    private WorkdayRadioGroupResolution resolveGroup(
            WorkdayRadioGroupInspection radioGroup
    ) {

        ApplicationQuestionFieldResolutionResult resolution =
                fieldResolutionService.resolve(radioGroup.identifiers());

        // A successfully mapped question must have a BOOLEAN answer.
        if (resolution.resolved()) {

            boolean booleanAnswerType =
                    "BOOLEAN".equalsIgnoreCase(
                            String.valueOf(resolution.answerType())
                    );

            boolean validBooleanValue =
                    resolution.answerValue() != null
                            && !resolution.answerValue().isNull()
                            && resolution.answerValue().isBoolean();

            if (!booleanAnswerType || !validBooleanValue) {

                resolution = new ApplicationQuestionFieldResolutionResult(
                        false,
                        resolution.questionKey(),
                        resolution.canonicalQuestion(),
                        resolution.answerType(),
                        resolution.answerValue(),
                        "ANSWER_TYPE_MISMATCH",
                        "Radio group requires a configured BOOLEAN answer."
                );
            }
        }

        return new WorkdayRadioGroupResolution(
                radioGroup.label(),
                radioGroup.required(),
                resolution
        );
    }
}