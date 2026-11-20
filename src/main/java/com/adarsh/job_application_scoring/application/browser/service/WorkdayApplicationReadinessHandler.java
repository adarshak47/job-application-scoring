
package com.adarsh.job_application_scoring.application.browser.service;

import com.adarsh.job_application_scoring.application.browser.dto.*;
import com.adarsh.job_application_scoring.application.data.dto.WorkdayApplicationData;
import com.adarsh.job_application_scoring.application.data.dto.WorkdayPersonalInformation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class WorkdayApplicationReadinessHandler {

    public WorkdayApplicationReadinessResult evaluate(
            WorkdayApplicationData applicationData,
            WorkdayApplicationStepInspectionResult formInspection,
            List<WorkdayRadioGroupInspection> radioInspections,
            List<WorkdayRadioGroupSelectionResult> radioSelectionResults
    ) {

        List<String> availableRequiredFields = new ArrayList<>();
        List<String> missingCandidateData = new ArrayList<>();
        List<String> unfilledRequiredFields = new ArrayList<>();
        List<String> blockingReasons = new ArrayList<>();

        if (applicationData == null) {
            return blocked("Candidate application data is unavailable.");
        }

        if (formInspection == null || !formInspection.applicationStepDetected()) {
            return blocked("Workday application step was not detected.");
        }

        if (formInspection.fields() == null) {
            return blocked("Workday form fields could not be inspected.");
        }

        for (WorkdayFormFieldInspection field : formInspection.fields()) {

            if (field == null || !field.required()) {
                continue;
            }

            String fieldLabel = hasText(field.label())
                    ? field.label()
                    : field.automationId();

            // Radio questions must have a configured and selected answer.
            if ("RADIO".equalsIgnoreCase(field.type())) {

                WorkdayRadioGroupSelectionResult selection =
                        findRadioSelection(
                                field,
                                radioInspections,
                                radioSelectionResults
                        );

                if (selection != null && selection.selected()) {

                    availableRequiredFields.add(fieldLabel);

                } else {

                    missingCandidateData.add(
                            fieldLabel + " (answer not configured or not selected)"
                    );

                    unfilledRequiredFields.add(fieldLabel);
                }

                continue;
            }

            // First check whether the field is actually populated in Workday.
            if (hasExistingValue(field)) {

                availableRequiredFields.add(fieldLabel);
                continue;
            }

            // If empty in Workday, check whether candidate data can fill it.
            String candidateValue = getCandidateValue(
                    field,
                    applicationData.personalInformation()
            );

            if (hasText(candidateValue)) {

                availableRequiredFields.add(fieldLabel);

                // Data exists, but the Workday field is still empty.
                unfilledRequiredFields.add(fieldLabel);

            } else {

                missingCandidateData.add(fieldLabel);
                unfilledRequiredFields.add(fieldLabel);
            }
        }

        boolean candidateDataReady = missingCandidateData.isEmpty();

        boolean formReady = unfilledRequiredFields.isEmpty();

        boolean readyToProceed =
                candidateDataReady && formReady;

        if (!candidateDataReady) {
            blockingReasons.add(
                    "Required candidate information or question answers are missing."
            );
        }

        if (!formReady) {
            blockingReasons.add(
                    "Some required Workday fields have not been populated and verified."
            );
        }

        String status;

        if (readyToProceed) {
            status = "READY_TO_PROCEED";
        } else if (!candidateDataReady) {
            status = "REQUIRED_DATA_MISSING";
        } else {
            status = "APPLICATION_BLOCKED";
        }

        return new WorkdayApplicationReadinessResult(
                status,
                candidateDataReady,
                formReady,
                readyToProceed,
                availableRequiredFields,
                missingCandidateData,
                unfilledRequiredFields,
                blockingReasons
        );
    }

    private WorkdayRadioGroupSelectionResult findRadioSelection(
            WorkdayFormFieldInspection field,
            List<WorkdayRadioGroupInspection> inspections,
            List<WorkdayRadioGroupSelectionResult> selectionResults
    ) {

        if (inspections == null || selectionResults == null) {
            return null;
        }

        for (int i = 0; i < inspections.size(); i++) {

            WorkdayRadioGroupInspection inspection = inspections.get(i);

            if (inspection == null || inspection.identifiers() == null) {
                continue;
            }

            boolean matches = inspection.identifiers().stream()
                    .filter(identifier -> identifier != null)
                    .anyMatch(identifier -> {

                        String value = identifier.fieldIdentifier();

                        if (!hasText(value)) {
                            return false;
                        }

                        return switch (identifier.identifierType()) {
                            case AUTOMATION_ID ->
                                    value.equals(field.automationId());

                            case FIELD_NAME ->
                                    value.equals(field.name());

                            case FKIT_ID -> false;
                        };
                    });

            if (matches && i < selectionResults.size()) {
                return selectionResults.get(i);
            }
        }

        return null;
    }

    private boolean hasExistingValue(WorkdayFormFieldInspection field) {

        if (hasText(field.value())) {
            return true;
        }

        if (field.checked()) {
            return true;
        }

        if (field.selectedValues() != null) {
            return field.selectedValues().stream()
                    .anyMatch(this::isMeaningfulSelectedValue);
        }

        return false;
    }

    private boolean isMeaningfulSelectedValue(String value) {

        if (!hasText(value)) {
            return false;
        }

        String normalized = value.trim().toLowerCase();

        return !normalized.equals("select one")
                && !normalized.equals("select")
                && !normalized.equals("none");
    }

    private String getCandidateValue(
            WorkdayFormFieldInspection field,
            WorkdayPersonalInformation personal
    ) {

        if (personal == null || !hasText(field.automationId())) {
            return null;
        }

        return switch (field.automationId()) {

            case "formField-legalName--firstName" ->
                    personal.legalName() == null
                            ? null : personal.legalName().givenName();

            case "formField-legalName--lastName" ->
                    personal.legalName() == null
                            ? null : personal.legalName().familyName();

            case "formField-addressLine1" ->
                    personal.address() == null
                            ? null : personal.address().line1();

            case "formField-city" ->
                    personal.address() == null
                            ? null : personal.address().city();

            case "formField-postalCode" ->
                    personal.address() == null
                            ? null : personal.address().postalCode();

            case "formField-phoneNumber" ->
                    personal.phone() == null
                            ? null : personal.phone().number();

            case "formField-phoneType" ->
                    personal.phone() == null
                            ? null : personal.phone().deviceType();

            case "formField-countryPhoneCode" ->
                    personal.phone() == null
                            ? null : personal.phone().countryCode();

            case "formField-country" ->
                    personal.country();

            default -> null;
        };
    }

    private WorkdayApplicationReadinessResult blocked(String reason) {

        return new WorkdayApplicationReadinessResult(
                "APPLICATION_BLOCKED",
                false,
                false,
                false,
                List.of(),
                List.of(),
                List.of(),
                List.of(reason)
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
