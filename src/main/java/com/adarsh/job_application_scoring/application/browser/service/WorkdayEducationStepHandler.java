
package com.adarsh.job_application_scoring.application.browser.service;

import com.adarsh.job_application_scoring.application.browser.dto.WorkdayEducationStepResult;
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
public class WorkdayEducationStepHandler {

    private static final String SECTION_SELECTOR =
            "[role='group'][aria-labelledby='Education-section']";


    private static final String PANEL_SELECTOR =
            ":scope > div[role='group']" +
                    "[aria-labelledby^='Education-']" +
                    "[aria-labelledby$='-panel']";

    private static final String ADD_BUTTON_SELECTOR =
            "button[data-automation-id='add-button']";

    private static final String FIELD_OF_STUDY_QUERY = "Computer";

    private static final List<String> FIELD_OF_STUDY_PRIORITY =
            List.of(
                    "Computer Science",
                    "Computer and Information Science",
                    "Computer Engineering"
            );

    private final WorkdayApplicationDataProvider workdayApplicationDataProvider;

    public WorkdayEducationStepHandler(
            WorkdayApplicationDataProvider workdayApplicationDataProvider
    ) {
        this.workdayApplicationDataProvider = workdayApplicationDataProvider;
    }

    public WorkdayEducationStepResult process(Page page) {

        WorkdayApplicationData applicationData =
                workdayApplicationDataProvider.getApplicationData();

        WorkdayCandidateCollections collections =
                applicationData.candidateCollections();

        List<WorkdayCandidateCollections.EducationData> educations =
                collections != null && collections.educations() != null
                        ? collections.educations()
                        : List.of();

        if (educations.isEmpty()) {
            return result(
                    false,
                    "NO_EDUCATION_DATA",
                    0,
                    0,
                    List.of("educations"),
                    List.of(),
                    "Candidate profile contains no education records."
            );
        }

        // Validate every record before interacting with Workday.
        List<String> missingData = validateCandidateData(educations);

        if (!missingData.isEmpty()) {
            return result(
                    false,
                    "MISSING_CANDIDATE_DATA",
                    educations.size(),
                    0,
                    missingData,
                    List.of(),
                    "Education filling was skipped because required candidate data is missing."
            );
        }

        Locator section = page.locator(SECTION_SELECTOR);

        if (section.count() != 1) {
            return result(
                    false,
                    "EDUCATION_SECTION_NOT_FOUND",
                    educations.size(),
                    0,
                    List.of(),
                    List.of(),
                    "Could not uniquely locate the Education section."
            );
        }

        Locator panels = section.locator(PANEL_SELECTOR);

        try {
            int existingPanelCount = panels.count();

            // Reveal the first Education form if Workday has not created it.
            if (existingPanelCount == 0) {
                clickSectionButton(section, "Add");

                panels.nth(0).waitFor(
                        new Locator.WaitForOptions()
                                .setState(WaitForSelectorState.VISIBLE)
                );
            }

            existingPanelCount = panels.count();

            if (existingPanelCount > educations.size()) {
                return result(
                        false,
                        "EXISTING_RECORD_COUNT_EXCEEDS_CANDIDATE_DATA",
                        educations.size(),
                        0,
                        List.of(),
                        List.of(),
                        "Workday displays more Education forms than the candidate profile contains."
                );
            }

            List<WorkdayEducationStepResult.EducationFillResult> recordResults =
                    new ArrayList<>();

            for (int index = 0; index < educations.size(); index++) {

                // Add another form only when the corresponding form does not exist.
                if (index >= panels.count()) {
                    clickSectionButton(section, "Add Another");

                    panels.nth(index).waitFor(
                            new Locator.WaitForOptions()
                                    .setState(WaitForSelectorState.VISIBLE)
                    );
                }

                Locator panel = panels.nth(index);

                WorkdayEducationStepResult.EducationFillResult recordResult =
                        fillEducation(
                                page,
                                panel,
                                educations.get(index),
                                index + 1
                        );

                recordResults.add(recordResult);

                if (!recordResult.successful()) {
                    return result(
                            false,
                            "EDUCATION_FILL_FAILED",
                            educations.size(),
                            index,
                            List.of(),
                            recordResults,
                            "Education record " + (index + 1)
                                    + " could not be filled and verified."
                    );
                }
            }

            return result(
                    true,
                    "ALL_EDUCATION_RECORDS_FILLED_AND_VERIFIED",
                    educations.size(),
                    recordResults.size(),
                    List.of(),
                    recordResults,
                    "All Education records were filled and verified."
            );

        } catch (Exception e) {

            log.error("Failed to process Education section", e);

            return result(
                    false,
                    "EDUCATION_PROCESSING_ERROR",
                    educations.size(),
                    0,
                    List.of(),
                    List.of(),
                    e.getMessage()
            );
        }
    }

    private List<String> validateCandidateData(
            List<WorkdayCandidateCollections.EducationData> educations
    ) {
        List<String> missing = new ArrayList<>();

        for (int i = 0; i < educations.size(); i++) {

            WorkdayCandidateCollections.EducationData education =
                    educations.get(i);

            if (education == null) {
                missing.add("educations[" + i + "]");
                continue;
            }

            if (isBlank(education.schoolOrUniversity())) {
                missing.add("educations[" + i + "].schoolOrUniversity");
            }

            if (isBlank(education.degree())) {
                missing.add("educations[" + i + "].degree");
            }

            if (isBlank(education.fieldOfStudy())) {
                missing.add("educations[" + i + "].fieldOfStudy");
            }

            if (education.startDate() == null) {
                missing.add("educations[" + i + "].startDate");
            }

            if (education.endDate() == null) {
                missing.add("educations[" + i + "].endDate");
            }
        }

        return missing;
    }

    private WorkdayEducationStepResult.EducationFillResult fillEducation(
            Page page,
            Locator panel,
            WorkdayCandidateCollections.EducationData education,
            int recordNumber
    ) {
        List<String> filledFields = new ArrayList<>();
        List<String> missingFields = new ArrayList<>();

        try {
            fillSchoolOrUniversity(
                    page,
                    panel,
                    education.schoolOrUniversity()
            );

            filledFields.add("School or University");

            selectDegree(
                    page,
                    panel,
                    education.degree()
            );
            filledFields.add("Degree");

            selectFieldOfStudy(
                    page,
                    panel,
                    education.fieldOfStudy()
            );
            filledFields.add("Field of Study");

            fillOptionalTextField(
                    panel,
                    "formField-gradeAverage",
                    education.cgpa(),
                    "Overall Result (GPA)",
                    filledFields
            );

            fillYearField(
                    panel,
                    "formField-firstYearAttended",
                    education.startDate()
            );
            filledFields.add("From Year");

            fillYearField(
                    panel,
                    "formField-lastYearAttended",
                    education.endDate()
            );
            filledFields.add("To Year");

            return new WorkdayEducationStepResult.EducationFillResult(
                    recordNumber,
                    true,
                    "FILLED_AND_VERIFIED",
                    filledFields,
                    missingFields,
                    null
            );

        } catch (Exception e) {

            log.error(
                    "Failed to fill Education record {}",
                    recordNumber,
                    e
            );

            return new WorkdayEducationStepResult.EducationFillResult(
                    recordNumber,
                    false,
                    "FILL_OR_VERIFICATION_FAILED",
                    filledFields,
                    missingFields,
                    e.getMessage()
            );
        }
    }

    private void fillOptionalTextField(
            Locator panel,
            String automationId,
            String value,
            String fieldName,
            List<String> filledFields
    ) {
        Locator field = panel.locator(
                "[data-automation-id='" + automationId + "']"
        );

        // This field is not present in every company's Workday form.
        if (field.count() == 0) {
            log.info("Optional field {} is not present. Skipping.", automationId);
            return;
        }

        fillTextField(panel, automationId, value, false);

        if (!isBlank(value)) {
            filledFields.add(fieldName);
        }
    }


    private void selectDegree(
            Page page,
            Locator panel,
            String candidateDegree
    ) {
        Locator field = panel.locator(
                "[data-automation-id='formField-degree']"
        );

        Locator dropdown = field.locator(
                "button[aria-haspopup='listbox']"
        );

        if (dropdown.count() != 1) {
            throw new IllegalStateException(
                    "Could not uniquely locate the Degree dropdown."
            );
        }

        String currentValue = dropdown.innerText().trim();

        // Resolve the desired degree from the actual available options.
        dropdown.click();

        Locator options = page.locator(
                "[role='listbox'] [role='option']:not([aria-disabled='true'])"
        );

        options.first().waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
        );

        Locator matchingOption = null;

        for (int i = 0; i < options.count(); i++) {
            Locator option = options.nth(i);
            String optionText = option.innerText().trim();

            if (matchesDegree(candidateDegree, optionText)) {
                matchingOption = option;
                break;
            }
        }

        if (matchingOption == null) {
            throw new IllegalStateException(
                    "Workday does not provide a matching Degree option for: "
                            + candidateDegree
            );
        }

        String expectedValue = matchingOption.innerText().trim();

        if (currentValue.equalsIgnoreCase(expectedValue)) {
            // Close dropdown without changing the existing selection.
            dropdown.click();
            return;
        }

        matchingOption.click();

        String actualValue = dropdown.innerText().trim();

        if (!actualValue.equalsIgnoreCase(expectedValue)) {
            throw new IllegalStateException(
                    "Degree selection verification failed. Expected: "
                            + expectedValue + ", actual: " + actualValue
            );
        }
    }

    private boolean matchesDegree(
            String candidateDegree,
            String optionText
    ) {
        String candidate = normalizeDegree(candidateDegree);
        String option = normalizeDegree(optionText);

        // Bachelor's degree and equivalents.
        if (isBachelorDegree(candidate)) {
            return option.equals("bachelors")
                    || option.contains("bachelor s or equivalent first degree")
                    || option.contains("bachelor or equivalent first degree");
        }

        // Master's degree and equivalents.
        if (isMasterDegree(candidate)) {
            return option.equals("masters")
                    || option.contains("masters or equivalent second degree")
                    || option.contains("master or equivalent second degree")
                    || option.contains("master of business administration");
        }

        // Associate degree.
        if (candidate.contains("associate")) {
            return option.equals("associates")
                    || option.contains("associate or equivalent short cycle");
        }

        // Doctorate.
        if (candidate.contains("doctor")
                || candidate.equals("phd")
                || candidate.equals("ph d")) {
            return option.equals("doctorate")
                    || option.contains("doctoral or equivalent research degree")
                    || option.contains("juris doctor");
        }

        // High school.
        if (candidate.contains("high school")) {
            return option.equals("high school")
                    || option.contains("high school or upper secondary");
        }

        // GED.
        if (candidate.equals("ged")) {
            return option.equals("ged")
                    || option.contains("high school or upper secondary");
        }

        // Fall back to exact normalized matching.
        return option.equals(candidate);
    }

    private boolean isBachelorDegree(String degree) {
        return degree.contains("bachelor")
                || degree.equals("be")
                || degree.equals("b e")
                || degree.equals("b e ")
                || degree.equals("beng")
                || degree.equals("b eng")
                || degree.equals("btech")
                || degree.equals("b tech")
                || degree.equals("bsc")
                || degree.equals("b sc")
                || degree.equals("ba")
                || degree.equals("b a");
    }

    private boolean isMasterDegree(String degree) {
        return degree.contains("master")
                || degree.equals("me")
                || degree.equals("m e")
                || degree.equals("meng")
                || degree.equals("mtech")
                || degree.equals("m tech")
                || degree.equals("msc")
                || degree.equals("mba")
                || degree.equals("ma");
    }




    private void selectFieldOfStudy(
            Page page,
            Locator panel,
            String candidateFieldOfStudy
    ) {
        Locator field = panel.locator(
                "[data-automation-id='formField-fieldOfStudy']"
        );

        Locator searchInput = field.locator(
                "input[data-uxi-widget-type='selectinput']"
        );

        if (searchInput.count() != 1) {
            throw new IllegalStateException(
                    "Could not uniquely locate the Field of Study search input."
            );
        }

        // Follow the same interaction used manually:
        // type Computer and press Enter to load the related options.
        searchInput.fill(FIELD_OF_STUDY_QUERY);
        searchInput.press("Enter");

        Locator activeList = page.locator(
                "[data-automation-id='activeListContainer'][role='listbox']"
        );

        activeList.waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
        );

        Locator options = activeList.locator(
                "[data-automation-id='menuItem'][role='option']"
        );

        if (options.count() == 0) {
            throw new IllegalStateException(
                    "Workday returned no Field of Study options for Computer."
            );
        }

        List<String> availableOptions = new ArrayList<>();

        for (int i = 0; i < options.count(); i++) {
            Locator option = options.nth(i);
            Locator label = option.locator(
                    "[data-automation-id='promptOption']"
            );

            if (label.count() == 1) {
                String optionLabel = label.getAttribute("data-automation-label");

                if (optionLabel != null && !optionLabel.isBlank()) {
                    availableOptions.add(optionLabel.trim());
                }
            }
        }

        String selectedOption = null;

        // Select the first exact match according to the agreed priority.
        for (String preferredOption : FIELD_OF_STUDY_PRIORITY) {
            if (availableOptions.stream().anyMatch(
                    available -> available.equalsIgnoreCase(preferredOption)
            )) {
                selectedOption = preferredOption;
                break;
            }
        }

        if (selectedOption == null) {
            throw new IllegalStateException(
                    "None of the accepted Field of Study options were found. "
                            + "Available options: " + availableOptions
            );
        }

        Locator matchingOption = activeList.locator(
                "[data-automation-id='menuItem'][role='option']"
                        + " [data-automation-id='promptOption']"
                        + "[data-automation-label='" + selectedOption + "']"
        );

        if (matchingOption.count() != 1) {
            throw new IllegalStateException(
                    "Could not uniquely locate Field of Study option: "
                            + selectedOption
            );
        }

        matchingOption.click();

        // Verify that Workday displays the selected value in the field.
        String fieldText = field.innerText();

        if (fieldText == null
                || !fieldText.toLowerCase().contains(
                selectedOption.toLowerCase()
        )) {
            throw new IllegalStateException(
                    "Field of Study selection verification failed. Expected: "
                            + selectedOption + ", field content: " + fieldText
            );
        }

        log.info(
                "Selected Field of Study '{}' for candidate value '{}'",
                selectedOption,
                candidateFieldOfStudy
        );
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

    private void fillYearField(
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

        Locator yearInput = field.locator(
                "input[data-automation-id='dateSectionYear-input']"
        );

        if (yearInput.count() != 1) {
            throw new IllegalStateException(
                    "Could not uniquely locate Year control for " + automationId
            );
        }

        String expectedYear = String.valueOf(date.getYear());

        yearInput.fill(expectedYear);

        String actualYear = yearInput.inputValue().trim();

        if (!actualYear.equals(expectedYear)) {
            throw new IllegalStateException(
                    "Year verification failed for " + automationId
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
                "Could not find Education button: " + expectedText
        );
    }

    private WorkdayEducationStepResult result(
            boolean successful,
            String status,
            int expectedCount,
            int processedCount,
            List<String> missingFields,
            List<WorkdayEducationStepResult.EducationFillResult> results,
            String signal
    ) {
        return new WorkdayEducationStepResult(
                successful,
                status,
                expectedCount,
                processedCount,
                missingFields,
                results,
                List.of(
                        signal == null
                                ? "No additional details available."
                                : signal
                )
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void fillSchoolOrUniversity(
            Page page,
            Locator panel,
            String candidateSchool
    ) {
        Locator textField = panel.locator(
                "[data-automation-id='formField-schoolName']"
        );

        if (textField.count() == 1) {

            fillTextField(
                    panel,
                    "formField-schoolName",
                    candidateSchool,
                    true
            );

            return;
        }

        Locator dropdownField = panel.locator(
                "[data-automation-id='formField-school']"
        );

        if (dropdownField.count() == 1) {

            selectSchoolFromDropdown(
                    page,
                    dropdownField,
                    candidateSchool
            );

            return;
        }

        throw new IllegalStateException(
                "Could not locate School or University field."
        );
    }
    private void selectSchoolFromDropdown(
            Page page,
            Locator field,
            String candidateSchool
    ) {
        Locator searchInput = field.locator(
                "input[data-uxi-widget-type='selectinput']"
        );

        if (searchInput.count() != 1) {
            throw new IllegalStateException(
                    "Could not uniquely locate School search input."
            );
        }

        // Search for the candidate's actual university.
        searchInput.fill(candidateSchool.trim());

        searchInput.press("Enter");

        Locator matchingOption = waitForSchoolOption(
                page,
                candidateSchool,
                5000
        );

        if (matchingOption != null) {

            matchingOption.click();

            verifySchoolSelection(field, candidateSchool);

            log.info(
                    "Selected college: {}",
                    candidateSchool
            );

            return;
        }

        log.info(
                "College '{}' not found. Trying Other.",
                candidateSchool
        );

        // Search for Other.
        searchInput.fill("Other");
        searchInput.press("Enter");

        // Verify the selected item from selectedItemList.
        verifySchoolSelection(field, "Other");

        log.info(
                "Selected Other for unavailable college: {}",
                candidateSchool
        );
    }


    private Locator waitForSchoolOption(
            Page page,
            String expectedValue,
            int timeoutMillis
    ) {
        long deadline = System.currentTimeMillis() + timeoutMillis;

        while (System.currentTimeMillis() < deadline) {

            Locator options = page.locator(
                    "[data-automation-id='menuItem'][role='option']:visible"
            );

            for (int i = 0; i < options.count(); i++) {

                Locator option = options.nth(i);

                Locator label = option.locator(
                        "[data-automation-id='promptOption']"
                );

                if (label.count() != 1) {
                    continue;
                }

                String actual = label.getAttribute(
                        "data-automation-label"
                );

                if (normalizeDegree(actual).equals(
                        normalizeDegree(expectedValue)
                )) {
                    return option;
                }
            }

            page.waitForTimeout(200);
        }

        return null;
    }

    private String normalizeDegree(String degree) {
        return degree == null
                ? ""
                : degree.toLowerCase()
                .replace('\u00a0', ' ')
                .replace('\u2019', '\'')
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }


    private void verifySchoolSelection(
            Locator schoolField,
            String expectedValue
    ) {
        Locator selectedItems = schoolField.locator(
                "[data-automation-id='selectedItem']"
        );

        selectedItems.first().waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(5000)
        );

        boolean found = false;

        for (int i = 0; i < selectedItems.count(); i++) {

            Locator label = selectedItems.nth(i).locator(
                    "[data-automation-id='promptOption']"
            );

            if (label.count() != 1) {
                continue;
            }

            String actual = label.getAttribute(
                    "data-automation-label"
            );

            if (normalizeDegree(actual).equals(
                    normalizeDegree(expectedValue)
            )) {
                found = true;
                break;
            }
        }

        if (!found) {
            throw new IllegalStateException(
                    "School selection verification failed. Expected: "
                            + expectedValue
            );
        }

        log.info(
                "School selection verified successfully: {}",
                expectedValue
        );
    }



}
