package com.adarsh.job_application_scoring.application.browser.service;

import com.adarsh.job_application_scoring.application.browser.dto.*;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkdayMyExperienceStepHandler {

    private final WorkdayWorkExperienceSectionHandler workExperienceSectionHandler;
    private final WorkdayEducationStepHandler educationStepHandler;
    private final WorkdayResumeUploadStepHandler resumeUploadStepHandler;
    private final WorkdayApplicationStepInspectionHandler applicationStepInspectionHandler;
    private final WorkdayMyExperienceReadinessHandler readinessHandler;
    private final WorkdaySaveAndContinueHandler saveAndContinueHandler;
    private final WorkdayPageRenderWaitHandler pageRenderWaitHandler;

    public WorkdayMyExperienceStepResult process(Page page) {

        // Step 2.1: Fill Work Experience
        WorkdayWorkExperienceSectionResult workExperienceResult =
                workExperienceSectionHandler.process(page);

        pageRenderWaitHandler.waitForPageRendered(page);

        // Step 2.2: Fill Education
        WorkdayEducationStepResult educationResult =
                educationStepHandler.process(page);

        pageRenderWaitHandler.waitForPageRendered(page);

        // Step 2.3: Upload Resume
        WorkdayResumeUploadStepResult resumeUploadResult =
                resumeUploadStepHandler.process(page);

        pageRenderWaitHandler.waitForPageRendered(page);

        // Step 2.4: Inspect the complete My Experience form
        WorkdayApplicationStepInspectionResult formInspectionResult =
                applicationStepInspectionHandler.inspect(page);

        // Step 2.5: Evaluate form readiness
        WorkdayApplicationReadinessResult readinessResult =
                readinessHandler.evaluate(formInspectionResult);

        WorkdaySaveAndContinueResult saveAndContinueResult;

        if (readinessResult.readyToProceed()) {

            saveAndContinueResult =
                    saveAndContinueHandler.clickAndVerify(page);

        } else {

            saveAndContinueResult = new WorkdaySaveAndContinueResult(
                    false,
                    false,
                    "SKIPPED_NOT_READY",
                    page.url(),
                    page.url(),
                    List.of(
                            "My Experience step is not ready to proceed."
                    )
            );
        }

        // Step 2.8: Return combined My Experience result
        return new WorkdayMyExperienceStepResult(
                workExperienceResult,
                educationResult,
                resumeUploadResult,
                formInspectionResult,
                readinessResult,
                saveAndContinueResult
        );
    }

}