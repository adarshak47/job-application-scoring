
package com.adarsh.job_application_scoring.application.browser.service;

import com.adarsh.job_application_scoring.application.browser.dto.WorkdayMyInformationFillResult;
import com.adarsh.job_application_scoring.application.data.dto.WorkdayApplicationData;
import com.adarsh.job_application_scoring.application.data.dto.WorkdayPersonalInformation;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class WorkdayMyInformationFillHandler {

    public WorkdayMyInformationFillResult fill(
            Page page,
            WorkdayApplicationData applicationData
    ) {
        List<String> filledFields = new ArrayList<>();
        List<String> missingFields = new ArrayList<>();
        List<String> signals = new ArrayList<>();

        if (applicationData == null
                || applicationData.personalInformation() == null) {

            return new WorkdayMyInformationFillResult(
                    false,
                    "CANDIDATE_DATA_NOT_AVAILABLE",
                    filledFields,
                    List.of("Personal information is not available."),
                    List.of("Candidate application data is missing.")
            );
        }

        WorkdayPersonalInformation personal =
                applicationData.personalInformation();

        if (personal.legalName() == null) {
            missingFields.add("Given Name(s)");
            missingFields.add("Family Name");
        } else {
            fillTextField(
                    page,
                    "formField-legalName--firstName",
                    "Given Name(s)",
                    personal.legalName().givenName(),
                    filledFields,
                    missingFields,
                    signals
            );

            fillTextField(
                    page,
                    "formField-legalName--lastName",
                    "Family Name",
                    personal.legalName().familyName(),
                    filledFields,
                    missingFields,
                    signals
            );
        }

        if (personal.address() == null) {
            missingFields.add("Address Line 1");
            missingFields.add("City");
            missingFields.add("Postal Code");
        } else {
            fillTextField(
                    page,
                    "formField-addressLine1",
                    "Address Line 1",
                    personal.address().line1(),
                    filledFields,
                    missingFields,
                    signals
            );

            fillTextField(
                    page,
                    "formField-city",
                    "City",
                    personal.address().city(),
                    filledFields,
                    missingFields,
                    signals
            );

            fillTextField(
                    page,
                    "formField-postalCode",
                    "Postal Code",
                    personal.address().postalCode(),
                    filledFields,
                    missingFields,
                    signals
            );
        }

        if (personal.phone() == null) {
            missingFields.add("Phone Number");
        } else {
            fillTextField(
                    page,
                    "formField-phoneNumber",
                    "Phone Number",
                    personal.phone().number(),
                    filledFields,
                    missingFields,
                    signals
            );
        }

        boolean successful = missingFields.isEmpty();

        String status = successful
                ? "ALL_REQUIRED_FIELDS_FILLED_AND_VERIFIED"
                : "SOME_REQUIRED_FIELDS_MISSING_OR_FAILED";

        signals.add(successful
                ? "ALL_TARGET_FIELDS_VERIFIED"
                : "ONE_OR_MORE_TARGET_FIELDS_NOT_VERIFIED");

        log.info(
                "Workday My Information filling completed. Verified: {}, Missing: {}",
                filledFields.size(),
                missingFields.size()
        );

        return new WorkdayMyInformationFillResult(
                successful,
                status,
                filledFields,
                missingFields,
                signals
        );
    }


    private void fillTextField(
            Page page,
            String automationId,
            String label,
            String candidateValue,
            List<String> filledFields,
            List<String> missingFields,
            List<String> signals
    ) {
        try {
            Locator field = page.locator(
                    "[data-automation-id='" + automationId + "']"
            ).first();

            if (field.count() == 0 || !field.isVisible()) {
                missingFields.add(label + " - Workday field not found");
                signals.add(label + ": WORKDAY_FIELD_NOT_FOUND");
                return;
            }

            Locator input = field.locator(
                    "input:not([type='hidden']), textarea"
            ).first();

            if (input.count() == 0 || !input.isVisible() || !input.isEnabled()) {
                missingFields.add(label + " - input is unavailable");
                signals.add(label + ": INPUT_NOT_AVAILABLE");
                return;
            }

            String existingValue = input.inputValue();

            // Never overwrite a value already present in Workday.
            if (existingValue != null && !existingValue.isBlank()) {
                if (candidateValue != null && !candidateValue.isBlank()
                        && !normalize(existingValue).equals(normalize(candidateValue))) {

                    missingFields.add(label + " - existing value differs from candidate data");
                    signals.add(label + ": EXISTING_VALUE_MISMATCH");
                    return;
                }

                filledFields.add(label);
                signals.add(label + ": ALREADY_POPULATED_AND_VERIFIED");
                return;
            }

            if (candidateValue == null || candidateValue.isBlank()) {
                missingFields.add(label + " - candidate data is missing");
                signals.add(label + ": CANDIDATE_VALUE_MISSING");
                return;
            }

            input.fill(candidateValue.trim());

            String actualValue = input.inputValue();

            if (normalize(actualValue).equals(normalize(candidateValue))) {
                filledFields.add(label);
                signals.add(label + ": FILLED_AND_VERIFIED");
            } else {
                missingFields.add(label + " - value verification failed");
                signals.add(label + ": VALUE_VERIFICATION_FAILED");
            }

        } catch (Exception exception) {
            log.warn(
                    "Could not fill Workday field {}. Reason: {}",
                    label,
                    exception.getMessage()
            );

            missingFields.add(label + " - filling failed");
            signals.add(label + ": FILL_FAILED");
        }
    }


    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
