package com.adarsh.job_application_scoring.application.browser.service;

import com.adarsh.job_application_scoring.application.browser.dto.WorkdayApplicationStepInspectionResult;
import com.adarsh.job_application_scoring.application.browser.dto.WorkdayFormFieldInspection;
import com.adarsh.job_application_scoring.application.browser.dto.WorkdaySaveAndContinueResult;
import com.adarsh.job_application_scoring.application.browser.dto.WorkdayVoluntaryDisclosuresStepResult;
import com.adarsh.job_application_scoring.candidate.dto.CandidateProfileResponse;
import com.adarsh.job_application_scoring.candidate.service.CandidateProfileService;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkdayVoluntaryDisclosuresStepHandler {

    private static final String GENDER_AUTOMATION_ID =
            "formField-gender";

    private static final String PRIVACY_ACKNOWLEDGEMENT_AUTOMATION_ID =
            "formField-acceptTermsAndAgreements";

    private final WorkdayApplicationStepInspectionHandler inspectionHandler;

    private final CandidateProfileService candidateProfileService;

    private final WorkdaySaveAndContinueHandler saveAndContinueHandler;

    public WorkdayVoluntaryDisclosuresStepResult process(Page page) {

        WorkdayApplicationStepInspectionResult inspection =
                inspectionHandler.inspect(page);

        if (!inspection.applicationStepDetected()) {
            return failure(
                    "VOLUNTARY_DISCLOSURES_STEP_NOT_DETECTED",
                    null,
                    false,
                    false,
                    inspection,
                    null
            );
        }

        // Find the required Gender dropdown.
        Optional<WorkdayFormFieldInspection> genderField =
                findField(inspection.fields(), GENDER_AUTOMATION_ID);

        if (genderField.isEmpty()
                || !genderField.get().required()
                || !"SINGLE_SELECT".equalsIgnoreCase(genderField.get().type())) {

            return failure(
                    "REQUIRED_GENDER_FIELD_NOT_FOUND_OR_UNSUPPORTED",
                    null,
                    false,
                    false,
                    inspection,
                    null
            );
        }

        // Find the required privacy acknowledgement checkbox.
        Optional<WorkdayFormFieldInspection> privacyField =
                findField(
                        inspection.fields(),
                        PRIVACY_ACKNOWLEDGEMENT_AUTOMATION_ID
                );

        if (privacyField.isEmpty()
                || !privacyField.get().required()
                || !"CHECKBOX".equalsIgnoreCase(privacyField.get().type())) {

            return failure(
                    "REQUIRED_PRIVACY_ACKNOWLEDGEMENT_NOT_FOUND_OR_UNSUPPORTED",
                    null,
                    false,
                    false,
                    inspection,
                    null
            );
        }

        // Retrieve the gender from the existing candidate profile.
        String gender;

        try {
            CandidateProfileResponse profile = candidateProfileService.get();

            if (profile.personalInformation() == null) {
                return failure(
                        "CANDIDATE_PERSONAL_INFORMATION_NOT_FOUND",
                        null,
                        false,
                        false,
                        inspection,
                        null
                );
            }

            gender = profile.personalInformation().gender();

        } catch (Exception exception) {

            log.error(
                    "Failed to retrieve candidate profile: {}",
                    exception.getMessage()
            );

            return failure(
                    "CANDIDATE_PROFILE_RETRIEVAL_FAILED",
                    null,
                    false,
                    false,
                    inspection,
                    null
            );
        }

        if (gender == null || gender.isBlank()) {
            return failure(
                    "CANDIDATE_GENDER_NOT_CONFIGURED",
                    null,
                    false,
                    false,
                    inspection,
                    null
            );
        }

        // Match the candidate's gender against the available Workday options.
        String expectedGender = resolveGenderOption(
                gender,
                genderField.get().options()
        );

        if (expectedGender == null) {
            log.warn(
                    "Candidate gender '{}' is not available in Workday options",
                    gender
            );

            return failure(
                    "GENDER_OPTION_NOT_SUPPORTED",
                    null,
                    false,
                    false,
                    inspection,
                    null
            );
        }

        // Select and verify gender.
        boolean genderSelected = selectDropdownOption(
                page,
                GENDER_AUTOMATION_ID,
                expectedGender
        );

        if (!genderSelected) {
            return failure(
                    "GENDER_SELECTION_FAILED",
                    expectedGender,
                    false,
                    false,
                    inspection,
                    null
            );
        }

        // User explicitly authorized checking this privacy acknowledgement.
        boolean privacyAcknowledged = checkPrivacyAcknowledgement(page);

        if (!privacyAcknowledged) {
            return failure(
                    "PRIVACY_ACKNOWLEDGEMENT_FAILED",
                    expectedGender,
                    true,
                    false,
                    inspectionHandler.inspect(page),
                    null
            );
        }

        // Inspect again to verify both fields.
        WorkdayApplicationStepInspectionResult finalInspection =
                inspectionHandler.inspect(page);

        boolean genderVerified = verifyGender(
                finalInspection,
                expectedGender
        );

        boolean privacyVerified = verifyPrivacyAcknowledgement(
                finalInspection
        );

        if (!genderVerified || !privacyVerified) {
            return failure(
                    "VOLUNTARY_DISCLOSURES_VERIFICATION_FAILED",
                    expectedGender,
                    genderVerified,
                    privacyVerified,
                    finalInspection,
                    null
            );
        }

        // Proceed only after both fields have been verified.
        WorkdaySaveAndContinueResult saveResult =
                saveAndContinueHandler.clickAndVerify(page);

        if (!saveResult.clicked() || !saveResult.transitionDetected()) {
            return failure(
                    "SAVE_AND_CONTINUE_FAILED",
                    expectedGender,
                    true,
                    true,
                    finalInspection,
                    saveResult
            );
        }

        log.info(
                "Workday Voluntary Disclosures completed successfully. Gender: {}",
                expectedGender
        );

        return new WorkdayVoluntaryDisclosuresStepResult(
                true,
                "VOLUNTARY_DISCLOSURES_COMPLETED",
                expectedGender,
                true,
                true,
                finalInspection,
                saveResult
        );
    }

    private Optional<WorkdayFormFieldInspection> findField(
            List<WorkdayFormFieldInspection> fields,
            String automationId
    ) {
        return fields.stream()
                .filter(field ->
                        automationId.equals(field.automationId())
                )
                .findFirst();
    }

    private String resolveGenderOption(
            String gender,
            List<com.adarsh.job_application_scoring.application.browser.dto.WorkdayFormOptionInspection> options
    ) {
        return options.stream()
                .map(option ->
                        option.label() == null ? "" : option.label().trim()
                )
                .filter(option -> !option.isBlank())
                .filter(option ->
                        normalize(option).equals(normalize(gender))
                )
                .findFirst()
                .orElse(null);
    }

    private boolean selectDropdownOption(
            Page page,
            String automationId,
            String expectedOption
    ) {
        try {
            Locator field = page.locator(
                    "[data-automation-id='" + automationId + "']"
            ).first();

            if (field.count() == 0 || !field.isVisible()) {
                return false;
            }

            Locator dropdown = field.locator(
                    "button[aria-haspopup='listbox']"
            ).first();

            if (dropdown.count() == 0 || !dropdown.isVisible()) {
                return false;
            }

            dropdown.click();

            Locator listboxes = page.locator("[role='listbox']:visible");

            if (listboxes.count() == 0) {
                return false;
            }

            Locator options = listboxes.last()
                    .locator("[role='option']");

            for (int i = 0; i < options.count(); i++) {

                Locator option = options.nth(i);

                String optionLabel = option.innerText().trim();

                if (normalize(optionLabel).equals(normalize(expectedOption))) {

                    if (!option.isVisible() || !option.isEnabled()) {
                        return false;
                    }

                    option.click();

                    String selectedLabel = dropdown.innerText().trim();

                    return normalize(selectedLabel)
                            .equals(normalize(expectedOption));
                }
            }

            return false;

        } catch (Exception exception) {

            log.error(
                    "Failed to select gender '{}': {}",
                    expectedOption,
                    exception.getMessage()
            );

            return false;
        }
    }

    private boolean checkPrivacyAcknowledgement(Page page) {
        try {
            Locator field = page.locator(
                    "[data-automation-id='" +
                            PRIVACY_ACKNOWLEDGEMENT_AUTOMATION_ID + "']"
            ).first();

            if (field.count() == 0 || !field.isVisible()) {
                return false;
            }

            Locator checkbox;

            if ("INPUT".equalsIgnoreCase(
                    field.evaluate("element => element.tagName").toString()
            )) {
                checkbox = field;
            } else {
                checkbox = field.locator(
                        "input[type='checkbox']"
                ).first();
            }

            if (checkbox.count() == 0 || !checkbox.isVisible()) {
                return false;
            }

            if (!checkbox.isChecked()) {
                checkbox.check();
            }

            return checkbox.isChecked();

        } catch (Exception exception) {

            log.error(
                    "Failed to check privacy acknowledgement: {}",
                    exception.getMessage()
            );

            return false;
        }
    }

    private boolean verifyGender(
            WorkdayApplicationStepInspectionResult inspection,
            String expectedGender
    ) {
        Optional<WorkdayFormFieldInspection> field =
                findField(inspection.fields(), GENDER_AUTOMATION_ID);

        if (field.isEmpty() || !field.get().checked()) {
            return false;
        }

        return field.get().selectedValues().stream()
                .anyMatch(value ->
                        normalize(value).equals(normalize(expectedGender))
                );
    }

    private boolean verifyPrivacyAcknowledgement(
            WorkdayApplicationStepInspectionResult inspection
    ) {
        Optional<WorkdayFormFieldInspection> field =
                findField(
                        inspection.fields(),
                        PRIVACY_ACKNOWLEDGEMENT_AUTOMATION_ID
                );

        return field.isPresent() && field.get().checked();
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private WorkdayVoluntaryDisclosuresStepResult failure(
            String status,
            String selectedGender,
            boolean genderVerified,
            boolean privacyVerified,
            WorkdayApplicationStepInspectionResult inspection,
            WorkdaySaveAndContinueResult saveResult
    ) {
        log.warn("Workday Voluntary Disclosures failed: {}", status);

        return new WorkdayVoluntaryDisclosuresStepResult(
                false,
                status,
                selectedGender,
                genderVerified,
                privacyVerified,
                inspection,
                saveResult
        );
    }
}