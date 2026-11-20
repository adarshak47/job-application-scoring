
package com.adarsh.job_application_scoring.application.browser.service;

import com.adarsh.job_application_scoring.application.browser.dto.WorkdayWorkExperienceSectionResult;
import com.adarsh.job_application_scoring.application.data.dto.WorkdayApplicationData;
import com.adarsh.job_application_scoring.application.data.dto.WorkdayCandidateCollections;
import com.adarsh.job_application_scoring.application.data.service.WorkdayApplicationDataProvider;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class WorkdayWorkExperienceSectionHandler {

    private static final String SECTION_SELECTOR =
            "[role='group'][aria-labelledby='Work-Experience-section']";

    private static final String PANEL_SELECTOR =
            ":scope > div[role='group']" +
                    "[aria-labelledby^='Work-Experience-']" +
                    "[aria-labelledby$='-panel']";

    private static final String ADD_BUTTON_SELECTOR =
            "button[data-automation-id='add-button']";

    private final WorkdayApplicationDataProvider workdayApplicationDataProvider;

    public WorkdayWorkExperienceSectionHandler(
            WorkdayApplicationDataProvider workdayApplicationDataProvider
    ) {
        this.workdayApplicationDataProvider = workdayApplicationDataProvider;
    }

    public WorkdayWorkExperienceSectionResult process(Page page) {

        WorkdayApplicationData applicationData =
                workdayApplicationDataProvider.getApplicationData();

        WorkdayCandidateCollections collections =
                applicationData.candidateCollections();

        List<WorkdayCandidateCollections.WorkExperienceData> experiences =
                collections != null && collections.workExperiences() != null
                        ? collections.workExperiences()
                        : List.of();

        if (experiences.isEmpty()) {
            return result(
                    false,
                    "NO_WORK_EXPERIENCE_DATA",
                    0,
                    0,
                    List.of("workExperiences"),
                    List.of(),
                    "Candidate profile contains no work experience records."
            );
        }

        // Validate all records before interacting with the browser.
        List<String> missingData = validateCandidateData(experiences);

        if (!missingData.isEmpty()) {
            return result(
                    false,
                    "MISSING_CANDIDATE_DATA",
                    experiences.size(),
                    0,
                    missingData,
                    List.of(),
                    "Work Experience filling was skipped because required candidate data is missing."
            );
        }

        Locator section = page.locator(SECTION_SELECTOR);

        if (section.count() != 1) {
            return result(
                    false,
                    "WORK_EXPERIENCE_SECTION_NOT_FOUND",
                    experiences.size(),
                    0,
                    List.of(),
                    List.of(),
                    "Could not uniquely locate the Work Experience section."
            );
        }

        Locator panels = section.locator(PANEL_SELECTOR);

        try {
            int existingPanelCount = panels.count();

            // Reveal the first experience form if Workday has not created it yet.
            if (existingPanelCount == 0) {
                clickSectionButton(section, "Add");
                panels.nth(0).waitFor(
                        new Locator.WaitForOptions()
                                .setState(WaitForSelectorState.VISIBLE)
                );
            }

            existingPanelCount = panels.count();

            if (existingPanelCount > experiences.size()) {
                return result(
                        false,
                        "EXISTING_RECORD_COUNT_EXCEEDS_CANDIDATE_DATA",
                        experiences.size(),
                        0,
                        List.of(),
                        List.of(),
                        "Workday displays more Work Experience forms than the candidate profile contains."
                );
            }

            List<WorkdayWorkExperienceSectionResult.WorkExperienceFillResult>
                    recordResults = new ArrayList<>();

            for (int index = 0; index < experiences.size(); index++) {

                // Add another form only when the corresponding form does not exist.
                if (index >= panels.count()) {
                    clickSectionButton(section, "Add Another");

                    panels.nth(index).waitFor(
                            new Locator.WaitForOptions()
                                    .setState(WaitForSelectorState.VISIBLE)
                    );
                }

                Locator panel = panels.nth(index);

                WorkdayWorkExperienceSectionResult.WorkExperienceFillResult
                        recordResult = fillWorkExperience(
                        panel,
                        experiences.get(index),
                        index + 1
                );

                recordResults.add(recordResult);

                if (!recordResult.successful()) {
                    return result(
                            false,
                            "WORK_EXPERIENCE_FILL_FAILED",
                            experiences.size(),
                            index,
                            List.of(),
                            recordResults,
                            "Work Experience record " + (index + 1)
                                    + " could not be filled and verified."
                    );
                }
            }

            return result(
                    true,
                    "ALL_WORK_EXPERIENCE_RECORDS_FILLED_AND_VERIFIED",
                    experiences.size(),
                    recordResults.size(),
                    List.of(),
                    recordResults,
                    "All Work Experience records were filled and verified."
            );

        } catch (Exception e) {

            log.error("Failed to process Work Experience section", e);

            return result(
                    false,
                    "WORK_EXPERIENCE_PROCESSING_ERROR",
                    experiences.size(),
                    0,
                    List.of(),
                    List.of(),
                    e.getMessage()
            );
        }
    }

    private List<String> validateCandidateData(
            List<WorkdayCandidateCollections.WorkExperienceData> experiences
    ) {
        List<String> missing = new ArrayList<>();

        for (int i = 0; i < experiences.size(); i++) {

            int recordNumber = i + 1;

            WorkdayCandidateCollections.WorkExperienceData experience =
                    experiences.get(i);

            if (experience == null) {
                missing.add("workExperiences[" + i + "]");
                continue;
            }

            if (isBlank(experience.jobTitle())) {
                missing.add("workExperiences[" + i + "].jobTitle");
            }

            if (isBlank(experience.company())) {
                missing.add("workExperiences[" + i + "].company");
            }

            if (experience.startDate() == null) {
                missing.add("workExperiences[" + i + "].startDate");
            }

            if (experience.currentlyWorkingHere() == null) {
                missing.add("workExperiences[" + i + "].currentlyWorkingHere");
            }

            if (Boolean.FALSE.equals(experience.currentlyWorkingHere())
                    && experience.endDate() == null) {
                missing.add("workExperiences[" + i + "].endDate");
            }
        }

        return missing;
    }

    private WorkdayWorkExperienceSectionResult.WorkExperienceFillResult
    fillWorkExperience(
            Locator panel,
            WorkdayCandidateCollections.WorkExperienceData experience,
            int recordNumber
    ) {

        List<String> filledFields = new ArrayList<>();
        List<String> missingFields = new ArrayList<>();

        try {
            fillTextField(
                    panel,
                    "formField-jobTitle",
                    experience.jobTitle(),
                    true
            );
            filledFields.add("Job Title");

            fillTextField(
                    panel,
                    "formField-companyName",
                    experience.company(),
                    true
            );
            filledFields.add("Company");

            fillTextField(
                    panel,
                    "formField-location",
                    experience.location(),
                    false
            );
            filledFields.add("Location");

            Locator currentlyWorkingCheckbox =
                    panel.locator(
                            "[data-automation-id='formField-currentlyWorkHere'] " +
                                    "input[type='checkbox']"
                    );

            if (experience.currentlyWorkingHere()) {
                currentlyWorkingCheckbox.check();
            } else {
                currentlyWorkingCheckbox.uncheck();
            }

            if (currentlyWorkingCheckbox.isChecked()
                    != experience.currentlyWorkingHere()) {
                throw new IllegalStateException(
                        "Currently working checkbox verification failed."
                );
            }

            filledFields.add("Currently Working Here");

            fillDateField(
                    panel,
                    "formField-startDate",
                    experience.startDate()
            );
            filledFields.add("Start Date");

            if (!experience.currentlyWorkingHere()) {
                fillDateField(
                        panel,
                        "formField-endDate",
                        experience.endDate()
                );
                filledFields.add("End Date");
            }

            fillTextField(
                    panel,
                    "formField-roleDescription",
                    experience.roleDescription(),
                    false
            );
            filledFields.add("Role Description");

            return new WorkdayWorkExperienceSectionResult.WorkExperienceFillResult(
                    recordNumber,
                    true,
                    "FILLED_AND_VERIFIED",
                    filledFields,
                    missingFields,
                    null
            );

        } catch (Exception e) {

            log.error(
                    "Failed to fill Work Experience record {}",
                    recordNumber,
                    e
            );

            return new WorkdayWorkExperienceSectionResult.WorkExperienceFillResult(
                    recordNumber,
                    false,
                    "FILL_OR_VERIFICATION_FAILED",
                    filledFields,
                    missingFields,
                    e.getMessage()
            );
        }
    }

    private void fillTextField(
            Locator panel,
            String automationId,
            String value,
            boolean required
    ) {
        Locator field = panel.locator(
                "[data-automation-id='" + automationId + "']"
        );

        Locator input = field.locator("input, textarea");

        if (input.count() != 1) {
            throw new IllegalStateException(
                    "Expected exactly one input for " + automationId
            );
        }

        String expected = value == null ? "" : value.trim();

        if (required && expected.isBlank()) {
            throw new IllegalStateException(
                    "Required candidate value is missing for " + automationId
            );
        }

        if (!expected.isBlank()) {
            input.fill(expected);
        }

        String actual = input.inputValue().trim();

        if (!actual.equals(expected)) {
            throw new IllegalStateException(
                    "Value verification failed for " + automationId
            );
        }
    }

    private void fillDateField(
            Locator panel,
            String automationId,
            LocalDate date
    ) {
        if (date == null) {
            throw new IllegalStateException(
                    "Date is missing for " + automationId
            );
        }

        Locator field = panel.locator(
                "[data-automation-id='" + automationId + "']"
        );

        Locator monthInput = field.locator(
                "input[data-automation-id='dateSectionMonth-input']"
        );

        Locator yearInput = field.locator(
                "input[data-automation-id='dateSectionYear-input']"
        );

        if (monthInput.count() != 1 || yearInput.count() != 1) {
            throw new IllegalStateException(
                    "Could not locate Month and Year controls for " + automationId
            );
        }

        String expectedMonth = String.valueOf(date.getMonthValue());
        String expectedYear = String.valueOf(date.getYear());

        monthInput.fill(expectedMonth);
        yearInput.fill(expectedYear);

        int actualMonth = Integer.parseInt(monthInput.inputValue());
        int actualYear = Integer.parseInt(yearInput.inputValue());

        if (actualMonth != date.getMonthValue()
                || actualYear != date.getYear()) {
            throw new IllegalStateException(
                    "Date verification failed for " + automationId
            );
        }
    }

    private void clickSectionButton(
            Locator section,
            String expectedText
    ) {
        Locator buttons = section.locator(ADD_BUTTON_SELECTOR);

        for (int i = 0; i < buttons.count(); i++) {

            Locator button = buttons.nth(i);

            String actualText = button.innerText().trim();

            if (actualText.equalsIgnoreCase(expectedText)) {
                button.click();
                return;
            }
        }

        throw new IllegalStateException(
                "Could not find Work Experience button: " + expectedText
        );
    }

    private WorkdayWorkExperienceSectionResult result(
            boolean successful,
            String status,
            int expectedCount,
            int processedCount,
            List<String> missingFields,
            List<WorkdayWorkExperienceSectionResult.WorkExperienceFillResult> results,
            String signal
    ) {
        return new WorkdayWorkExperienceSectionResult(
                successful,
                status,
                expectedCount,
                processedCount,
                missingFields,
                results,
                List.of(signal == null ? "No additional details available." : signal)
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
