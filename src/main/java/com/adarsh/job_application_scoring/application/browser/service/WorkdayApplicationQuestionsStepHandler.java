package com.adarsh.job_application_scoring.application.browser.service;

import com.adarsh.job_application_scoring.application.browser.dto.*;
import com.adarsh.job_application_scoring.application.question.dto.ApplicationQuestionResolutionRequest;
import com.adarsh.job_application_scoring.application.question.dto.ApplicationQuestionResolutionResponse;
import com.adarsh.job_application_scoring.application.question.dto.ResolvedApplicationQuestion;
import com.adarsh.job_application_scoring.application.question.model.AnswerType;
import com.adarsh.job_application_scoring.application.question.service.ApplicationQuestionResolutionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkdayApplicationQuestionsStepHandler {

    private final WorkdayApplicationStepInspectionHandler inspectionHandler;
    private final ApplicationQuestionResolutionService resolutionService;
    private final WorkdaySaveAndContinueHandler saveAndContinueHandler;

    public WorkdayApplicationQuestionsStepResult process(Page page) {

        WorkdayApplicationStepInspectionResult inspection =
                inspectionHandler.inspect(page);

        if (!inspection.applicationStepDetected()) {
            return failure(
                    "APPLICATION_QUESTIONS_STEP_NOT_DETECTED",
                    null,
                    List.of(),
                    inspection,
                    null
            );
        }

        List<WorkdayFormFieldInspection> requiredFields =
                inspection.fields().stream()
                        .filter(WorkdayFormFieldInspection::required)
                        .toList();

        if (requiredFields.isEmpty()) {
            return failure(
                    "NO_REQUIRED_QUESTIONS_FOUND",
                    null,
                    List.of(),
                    inspection,
                    null
            );
        }

        // This first implementation handles required dropdown questions.
        List<String> questionLabels = new ArrayList<>();

        for (WorkdayFormFieldInspection field : requiredFields) {

            if (!"SINGLE_SELECT".equalsIgnoreCase(field.type())
                    || field.label().isBlank()) {

                return failure(
                        "UNSUPPORTED_REQUIRED_FIELD",
                        null,
                        List.of(),
                        inspection,
                        null
                );
            }

            questionLabels.add(field.label());
        }

        // Resolve all questions against the existing database catalogue.
        ApplicationQuestionResolutionResponse resolutionResult =
                resolutionService.resolve(
                        new ApplicationQuestionResolutionRequest(questionLabels)
                );

        if (!resolutionResult.canProceed()) {

            log.warn(
                    "Cannot proceed with Workday Application Questions. " +
                            "Unknown: {}, Invalid: {}",
                    resolutionResult.unknownQuestions(),
                    resolutionResult.invalidQuestions()
            );

            return failure(
                    "QUESTION_RESOLUTION_FAILED",
                    resolutionResult,
                    List.of(),
                    inspection,
                    null
            );
        }

        List<WorkdayApplicationQuestionAnswerResult> answerResults =
                new ArrayList<>();

        // Fill every required question.
        for (WorkdayFormFieldInspection field : requiredFields) {

            Optional<ResolvedApplicationQuestion> resolvedQuestion =
                    resolutionResult.resolvedQuestions()
                            .stream()
                            .filter(question ->
                                    question.originalQuestion()
                                            .equals(field.label())
                            )
                            .findFirst();

            if (resolvedQuestion.isEmpty()) {

                answerResults.add(new WorkdayApplicationQuestionAnswerResult(
                        field.label(),
                        "",
                        "",
                        false,
                        "RESOLVED_QUESTION_NOT_FOUND"
                ));

                return failure(
                        "QUESTION_RESOLUTION_INCOMPLETE",
                        resolutionResult,
                        answerResults,
                        inspection,
                        null
                );
            }

            ResolvedApplicationQuestion question = resolvedQuestion.get();

            String expectedOption = resolveBooleanOption(
                    question.answerType(),
                    question.answerValue(),
                    field.options()
            );

            if (expectedOption == null) {

                answerResults.add(new WorkdayApplicationQuestionAnswerResult(
                        field.label(),
                        question.questionKey(),
                        "",
                        false,
                        "ANSWER_OPTION_NOT_SUPPORTED"
                ));

                return failure(
                        "ANSWER_OPTION_NOT_SUPPORTED",
                        resolutionResult,
                        answerResults,
                        inspection,
                        null
                );
            }

            boolean selected = selectDropdownOption(
                    page,
                    field.automationId(),
                    expectedOption
            );

            if (!selected) {

                answerResults.add(new WorkdayApplicationQuestionAnswerResult(
                        field.label(),
                        question.questionKey(),
                        expectedOption,
                        false,
                        "OPTION_SELECTION_FAILED"
                ));

                return failure(
                        "ANSWER_SELECTION_FAILED",
                        resolutionResult,
                        answerResults,
                        inspection,
                        null
                );
            }

            answerResults.add(new WorkdayApplicationQuestionAnswerResult(
                    field.label(),
                    question.questionKey(),
                    expectedOption,
                    true,
                    "ANSWER_SELECTED"
            ));
        }

        // Inspect the form again to verify all selections.
        WorkdayApplicationStepInspectionResult finalInspection =
                inspectionHandler.inspect(page);

        boolean allAnswersVerified = verifyAnswers(
                requiredFields,
                answerResults,
                finalInspection
        );

        if (!allAnswersVerified) {

            return failure(
                    "ANSWER_VERIFICATION_FAILED",
                    resolutionResult,
                    answerResults,
                    finalInspection,
                    null
            );
        }

        // Only proceed after all required answers have been verified.
        WorkdaySaveAndContinueResult saveResult =
                saveAndContinueHandler.clickAndVerify(page);

        if (!saveResult.clicked() || !saveResult.transitionDetected()) {

            return failure(
                    "SAVE_AND_CONTINUE_FAILED",
                    resolutionResult,
                    answerResults,
                    finalInspection,
                    saveResult
            );
        }

        log.info(
                "Workday Application Questions completed successfully. " +
                        "Answered questions: {}",
                answerResults.size()
        );

        return new WorkdayApplicationQuestionsStepResult(
                true,
                "APPLICATION_QUESTIONS_COMPLETED",
                resolutionResult,
                List.copyOf(answerResults),
                finalInspection,
                saveResult
        );
    }

    private String resolveBooleanOption(
            AnswerType answerType,
            JsonNode answerValue,
            List<WorkdayFormOptionInspection> options
    ) {

        if (answerType != AnswerType.BOOLEAN
                || answerValue == null
                || !answerValue.isBoolean()) {
            return null;
        }

        String preferredOption = answerValue.asBoolean()
                ? "Yes"
                : "No";

        String alternativeOption = answerValue.asBoolean()
                ? "Opt-In"
                : "Opt-Out";

        // Prefer Yes/No when available.
        Optional<String> preferred = findOption(
                options,
                preferredOption
        );

        if (preferred.isPresent()) {
            return preferred.get();
        }

        // Consent questions may use Opt-In/Opt-Out instead.
        return findOption(options, alternativeOption)
                .orElse(null);
    }

    private Optional<String> findOption(
            List<WorkdayFormOptionInspection> options,
            String expectedOption
    ) {

        return options.stream()
                .map(WorkdayFormOptionInspection::label)
                .filter(label -> normalize(label).equals(normalize(expectedOption)))
                .findFirst();
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

            Locator listbox = listboxes.last();
            Locator options = listbox.locator("[role='option']");

            for (int i = 0; i < options.count(); i++) {

                Locator option = options.nth(i);

                String optionLabel = option.innerText().trim();

                if (normalize(optionLabel).equals(normalize(expectedOption))) {

                    if (!option.isVisible() || !option.isEnabled()) {
                        return false;
                    }

                    option.click();

                    // Verify the dropdown displays the selected option.
                    String selectedLabel = dropdown.innerText().trim();

                    return normalize(selectedLabel)
                            .equals(normalize(expectedOption));
                }
            }

            log.warn(
                    "Expected option '{}' was not found for field '{}'",
                    expectedOption,
                    automationId
            );

            return false;

        } catch (Exception exception) {

            log.error(
                    "Failed to select option '{}' for field '{}': {}",
                    expectedOption,
                    automationId,
                    exception.getMessage()
            );

            return false;
        }
    }

    private boolean verifyAnswers(
            List<WorkdayFormFieldInspection> originalFields,
            List<WorkdayApplicationQuestionAnswerResult> answerResults,
            WorkdayApplicationStepInspectionResult finalInspection
    ) {

        if (answerResults.size() != originalFields.size()) {
            return false;
        }

        for (WorkdayFormFieldInspection originalField : originalFields) {

            Optional<WorkdayFormFieldInspection> inspectedField =
                    finalInspection.fields().stream()
                            .filter(field ->
                                    field.automationId().equals(
                                            originalField.automationId()
                                    )
                            )
                            .findFirst();

            if (inspectedField.isEmpty()) {
                return false;
            }

            WorkdayFormFieldInspection field = inspectedField.get();

            if (!field.checked()) {
                return false;
            }

            Optional<WorkdayApplicationQuestionAnswerResult> answerResult =
                    answerResults.stream()
                            .filter(answer ->
                                    answer.question().equals(originalField.label())
                            )
                            .findFirst();

            if (answerResult.isEmpty()) {
                return false;
            }

            String expectedOption = answerResult.get().selectedOption();

            boolean selectedOptionMatches = field.selectedValues().stream()
                    .anyMatch(value ->
                            normalize(value).equals(normalize(expectedOption))
                    );

            if (!selectedOptionMatches) {
                return false;
            }
        }

        return true;
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    private WorkdayApplicationQuestionsStepResult failure(
            String status,
            ApplicationQuestionResolutionResponse resolutionResult,
            List<WorkdayApplicationQuestionAnswerResult> answerResults,
            WorkdayApplicationStepInspectionResult inspection,
            WorkdaySaveAndContinueResult saveResult
    ) {

        log.warn("Workday Application Questions failed: {}", status);

        return new WorkdayApplicationQuestionsStepResult(
                false,
                status,
                resolutionResult,
                List.copyOf(answerResults),
                inspection,
                saveResult
        );
    }
}