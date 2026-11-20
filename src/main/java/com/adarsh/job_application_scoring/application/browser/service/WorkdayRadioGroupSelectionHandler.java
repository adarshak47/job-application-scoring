
package com.adarsh.job_application_scoring.application.browser.service;

import com.adarsh.job_application_scoring.application.browser.dto.WorkdayRadioGroupInspection;
import com.adarsh.job_application_scoring.application.browser.dto.WorkdayRadioGroupResolution;
import com.adarsh.job_application_scoring.application.browser.dto.WorkdayRadioGroupSelectionResult;
import com.adarsh.job_application_scoring.application.question.model.ApplicationQuestionFieldIdentifierType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class WorkdayRadioGroupSelectionHandler {

    public List<WorkdayRadioGroupSelectionResult> select(
            Page page,
            List<WorkdayRadioGroupInspection> inspections,
            List<WorkdayRadioGroupResolution> resolutions
    ) {

        List<WorkdayRadioGroupSelectionResult> results = new ArrayList<>();

        if (inspections == null || inspections.isEmpty()) {
            return results;
        }

        if (resolutions == null || inspections.size() != resolutions.size()) {

            results.add(new WorkdayRadioGroupSelectionResult(
                    null,
                    true,
                    false,
                    null,
                    null,
                    "RESOLUTION_DATA_MISMATCH",
                    "Radio-group inspection and resolution results do not match."
            ));

            return results;
        }

        boolean processingBlocked = false;

        for (int i = 0; i < inspections.size(); i++) {

            WorkdayRadioGroupInspection inspection = inspections.get(i);
            WorkdayRadioGroupResolution groupResolution = resolutions.get(i);

            if (inspection == null) {
                results.add(new WorkdayRadioGroupSelectionResult(
                        null,
                        true,
                        false,
                        null,
                        null,
                        "INVALID_INSPECTION",
                        "Radio-group inspection data is missing."
                ));

                processingBlocked = true;
                continue;
            }

            if (processingBlocked) {

                results.add(new WorkdayRadioGroupSelectionResult(
                        inspection.label(),
                        inspection.required(),
                        false,
                        null,
                        null,
                        "NOT_PROCESSED",
                        "Processing stopped because a previous required question could not be answered safely."
                ));

                continue;
            }

            // Step 1: Verify that the question was resolved
            if (groupResolution == null
                    || groupResolution.resolution() == null
                    || !groupResolution.resolution().resolved()) {

                String status = inspection.required()
                        ? "REQUIRED_QUESTION_UNRESOLVED"
                        : "OPTIONAL_QUESTION_SKIPPED";

                String message = inspection.required()
                        ? "Required radio question could not be resolved. Application processing must stop."
                        : "Optional radio question could not be resolved. It was left unanswered.";

                results.add(new WorkdayRadioGroupSelectionResult(
                        inspection.label(),
                        inspection.required(),
                        false,
                        null,
                        null,
                        status,
                        message
                ));

                if (inspection.required()) {
                    processingBlocked = true;
                }

                continue;
            }

            // Step 2: Validate the database answer
            var resolution = groupResolution.resolution();

            if (!"BOOLEAN".equalsIgnoreCase(
                    String.valueOf(resolution.answerType()))
                    || resolution.answerValue() == null
                    || !resolution.answerValue().isBoolean()) {

                results.add(new WorkdayRadioGroupSelectionResult(
                        inspection.label(),
                        inspection.required(),
                        false,
                        null,
                        null,
                        "INVALID_ANSWER_TYPE",
                        "Radio group requires a configured BOOLEAN answer."
                ));

                processingBlocked = true;
                continue;
            }

            // Step 3: Convert BOOLEAN answer into radio option value
            String targetValue = Boolean.toString(
                    resolution.answerValue().asBoolean()
            );

            // Step 4: Validate inspected radio options
            if (inspection.options() == null) {

                results.add(new WorkdayRadioGroupSelectionResult(
                        inspection.label(),
                        inspection.required(),
                        false,
                        targetValue,
                        null,
                        "RADIO_OPTIONS_MISSING",
                        "Radio options could not be inspected."
                ));

                processingBlocked = true;
                continue;
            }

            var matchingOptions = inspection.options()
                    .stream()
                    .filter(option -> option != null
                            && targetValue.equalsIgnoreCase(option.value()))
                    .toList();

            if (matchingOptions.size() != 1) {

                results.add(new WorkdayRadioGroupSelectionResult(
                        inspection.label(),
                        inspection.required(),
                        false,
                        targetValue,
                        null,
                        "RADIO_OPTION_NOT_FOUND",
                        "Could not find exactly one radio option matching the configured answer."
                ));

                processingBlocked = true;
                continue;
            }

            var targetOption = matchingOptions.get(0);

            try {

                // Step 5: Locate the matching radio input using stable identifiers
                Locator radio = findRadioInput(page, inspection, targetValue);

                if (radio == null || radio.count() != 1) {

                    results.add(new WorkdayRadioGroupSelectionResult(
                            inspection.label(),
                            inspection.required(),
                            false,
                            targetValue,
                            targetOption.label(),
                            "RADIO_OPTION_NOT_FOUND",
                            "Could not uniquely locate the matching radio input."
                    ));

                    processingBlocked = true;
                    continue;
                }

                // Step 6: Select the option if it is not already selected
                if (!radio.isChecked()) {
                    radio.check();
                }

                // Step 7: Verify the selection
                boolean selected = radio.isChecked();

                if (!selected) {

                    results.add(new WorkdayRadioGroupSelectionResult(
                            inspection.label(),
                            inspection.required(),
                            false,
                            targetValue,
                            targetOption.label(),
                            "SELECTION_VERIFICATION_FAILED",
                            "Radio option was not checked after selection."
                    ));

                    processingBlocked = true;
                    continue;
                }

                results.add(new WorkdayRadioGroupSelectionResult(
                        inspection.label(),
                        inspection.required(),
                        true,
                        targetValue,
                        targetOption.label(),
                        "RADIO_OPTION_SELECTED",
                        "Radio option successfully selected and verified."
                ));

            } catch (PlaywrightException exception) {

                log.error(
                        "Failed to select Workday radio option for question: {}",
                        inspection.label(),
                        exception
                );

                results.add(new WorkdayRadioGroupSelectionResult(
                        inspection.label(),
                        inspection.required(),
                        false,
                        targetValue,
                        targetOption.label(),
                        "RADIO_SELECTION_FAILED",
                        "Playwright failed to select or verify the radio option."
                ));

                processingBlocked = true;
            }
        }

        return results;
    }

    private Locator findRadioInput(
            Page page,
            WorkdayRadioGroupInspection inspection,
            String targetValue
    ) {

        if (inspection.identifiers() == null) {
            return null;
        }

        // First preference: Workday automation ID
        for (var identifier : inspection.identifiers()) {

            if (identifier.identifierType()
                    == ApplicationQuestionFieldIdentifierType.AUTOMATION_ID) {

                String selector = "[data-automation-id=\""
                        + escapeCssAttributeValue(identifier.fieldIdentifier())
                        + "\"]";

                Locator wrapper = page.locator(selector);

                if (wrapper.count() == 1) {
                    return wrapper.locator(
                            "input[type='radio'][value=\"" + targetValue + "\"]"
                    );
                }
            }
        }

        // Second preference: Workday field-kit ID
        for (var identifier : inspection.identifiers()) {

            if (identifier.identifierType()
                    == ApplicationQuestionFieldIdentifierType.FKIT_ID) {

                String selector = "[data-fkit-id=\""
                        + escapeCssAttributeValue(identifier.fieldIdentifier())
                        + "\"]";

                Locator wrapper = page.locator(selector);

                if (wrapper.count() == 1) {
                    return wrapper.locator(
                            "input[type='radio'][value=\"" + targetValue + "\"]"
                    );
                }
            }
        }

        // Final fallback: field name
        for (var identifier : inspection.identifiers()) {

            if (identifier.identifierType()
                    == ApplicationQuestionFieldIdentifierType.FIELD_NAME) {

                String selector = "input[type='radio'][name=\""
                        + escapeCssAttributeValue(identifier.fieldIdentifier())
                        + "\"][value=\"" + targetValue + "\"]";

                return page.locator(selector);
            }
        }

        return null;
    }

    private String escapeCssAttributeValue(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
