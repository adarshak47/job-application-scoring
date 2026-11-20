package com.adarsh.job_application_scoring.application.browser.service;

import com.adarsh.job_application_scoring.application.browser.dto.WorkdayApplicationReadinessResult;
import com.adarsh.job_application_scoring.application.browser.dto.WorkdayApplicationStepInspectionResult;
import com.adarsh.job_application_scoring.application.browser.dto.WorkdayFormFieldInspection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class WorkdayMyExperienceReadinessHandler {

    private static final List<String> EMPTY_SELECT_VALUES = List.of(
            "",
            "select",
            "select one",
            "none",
            "choose"
    );

    public WorkdayApplicationReadinessResult evaluate(
            WorkdayApplicationStepInspectionResult inspectionResult
    ) {
        List<String> availableRequiredFields = new ArrayList<>();
        List<String> unfilledRequiredFields = new ArrayList<>();
        List<String> blockingReasons = new ArrayList<>();

        if (inspectionResult == null) {
            blockingReasons.add("My Experience form inspection result is missing.");

            return createResult(
                    false,
                    availableRequiredFields,
                    unfilledRequiredFields,
                    blockingReasons
            );
        }

        if (!inspectionResult.applicationStepDetected()) {
            blockingReasons.add("My Experience application step was not detected.");

            return createResult(
                    false,
                    availableRequiredFields,
                    unfilledRequiredFields,
                    blockingReasons
            );
        }

        List<WorkdayFormFieldInspection> fields =
                inspectionResult.fields() != null
                        ? inspectionResult.fields()
                        : List.of();

        boolean resumeAttachmentPresent =
                hasUploadedResumeEvidence(inspectionResult.visibleButtons());

        for (WorkdayFormFieldInspection field : fields) {

            if (field == null || !field.required()) {
                continue;
            }

            String fieldName = resolveFieldName(field);

            if (isResumeUploadField(field)
                    && resumeAttachmentPresent) {

                availableRequiredFields.add(fieldName);
                continue;
            }

            if (hasExistingValue(field)) {
                availableRequiredFields.add(fieldName);
            } else {
                unfilledRequiredFields.add(fieldName);
            }
        }

        if (!unfilledRequiredFields.isEmpty()) {
            blockingReasons.add(
                    "Some required My Experience fields are not populated."
            );
        }

        boolean ready = unfilledRequiredFields.isEmpty();

        return createResult(
                ready,
                availableRequiredFields,
                unfilledRequiredFields,
                blockingReasons
        );
    }

    private boolean hasExistingValue(WorkdayFormFieldInspection field) {

        if (field.checked()) {
            return true;
        }

        if (isMeaningfulValue(field.value())) {
            return true;
        }

        if (field.selectedValues() != null) {
            return field.selectedValues()
                    .stream()
                    .anyMatch(this::isMeaningfulValue);
        }

        return false;
    }

    private boolean isMeaningfulValue(String value) {

        if (value == null) {
            return false;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);

        return !EMPTY_SELECT_VALUES.contains(normalized);
    }

    private boolean isResumeUploadField(WorkdayFormFieldInspection field) {

        String automationId = normalize(field.automationId());
        String fieldType = normalize(field.type());
        String label = normalize(field.label());

        return fieldType.contains("file")
                || fieldType.contains("upload")
                || automationId.contains("resume")
                || label.contains("resume");
    }

    private boolean hasUploadedResumeEvidence(List<String> visibleButtons) {

        if (visibleButtons == null) {
            return false;
        }

        return visibleButtons.stream()
                .filter(button -> button != null)
                .map(button -> button.trim().toLowerCase(Locale.ROOT))
                .anyMatch(button ->
                        button.startsWith("delete ")
                                && button.endsWith(".pdf")
                );
    }

    private String resolveFieldName(WorkdayFormFieldInspection field) {

        if (field.label() != null && !field.label().isBlank()) {
            return field.label();
        }

        if (field.automationId() != null
                && !field.automationId().isBlank()) {
            return field.automationId();
        }

        if (field.name() != null && !field.name().isBlank()) {
            return field.name();
        }

        return "Unknown required field";
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }

    private WorkdayApplicationReadinessResult createResult(
            boolean ready,
            List<String> availableRequiredFields,
            List<String> unfilledRequiredFields,
            List<String> blockingReasons
    ) {
        return new WorkdayApplicationReadinessResult(
                ready ? "READY_TO_PROCEED" : "BLOCKED",
                ready,
                ready,
                ready,
                List.copyOf(availableRequiredFields),
                List.of(),
                List.copyOf(unfilledRequiredFields),
                List.copyOf(blockingReasons)
        );
    }
}