
package com.adarsh.job_application_scoring.application.browser.service;

import com.adarsh.job_application_scoring.application.browser.dto.*;
import com.adarsh.job_application_scoring.application.browser.model.*;
import com.adarsh.job_application_scoring.application.data.dto.WorkdayApplicationData;
import com.adarsh.job_application_scoring.application.data.service.WorkdayApplicationDataProvider;
import com.microsoft.playwright.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkdayMyInformationStepHandler {


    private final WorkdaySourceSelectionHandler workdaySourceSelectionHandler;
    private final WorkdayRadioGroupInspectionHandler workdayRadioGroupInspectionHandler;
    private final WorkdayRadioGroupResolutionHandler workdayRadioGroupResolutionHandler;
    private final WorkdayRadioGroupSelectionHandler workdayRadioGroupSelectionHandler;
    private final WorkdayApplicationDataProvider workdayApplicationDataProvider;
    private final WorkdayMyInformationFillHandler workdayMyInformationFillHandler;
    private final WorkdayApplicationStepInspectionHandler applicationStepInspectionHandler;
    private final WorkdayApplicationReadinessHandler workdayApplicationReadinessHandler;
    private final WorkdaySaveAndContinueHandler workdaySaveAndContinueHandler;
    private final WorkdayPageRenderWaitHandler pageRenderWaitHandler;


    public WorkdayMyInformationStepResult process(Page page) {

        // Step 1: Select application source
        WorkdaySourceSelectionResult sourceSelectionResult =
                workdaySourceSelectionHandler.selectSource(page);

        if (sourceSelectionResult.selected()) {
            pageRenderWaitHandler.waitForPageRendered(page);
        }

        // Step 2: Inspect radio groups
        List<WorkdayRadioGroupInspection> radioGroupInspections =
                workdayRadioGroupInspectionHandler.inspect(page);

        // Step 3: Resolve radio answers
        List<WorkdayRadioGroupResolution> radioGroupResolutions =
                workdayRadioGroupResolutionHandler.resolve(
                        radioGroupInspections
                );

        // Step 4: Select and verify radio answers
        List<WorkdayRadioGroupSelectionResult> radioGroupSelectionResults =
                workdayRadioGroupSelectionHandler.select(
                        page,
                        radioGroupInspections,
                        radioGroupResolutions
                );

        if (radioGroupSelectionResults.stream()
                .anyMatch(WorkdayRadioGroupSelectionResult::selected)) {
            pageRenderWaitHandler.waitForPageRendered(page);
        }

        // Step 5: Retrieve candidate application data
        WorkdayApplicationData applicationData =
                workdayApplicationDataProvider.getApplicationData();

        // Step 6: Fill My Information fields
        WorkdayMyInformationFillResult myInformationFillResult =
                workdayMyInformationFillHandler.fill(
                        page,
                        applicationData
                );

        if (!myInformationFillResult.filledFields().isEmpty()) {
            pageRenderWaitHandler.waitForPageRendered(page);
        }

        // Step 7: Inspect the updated form
        WorkdayApplicationStepInspectionResult formInspectionResult =
                applicationStepInspectionHandler.inspect(page);

        // Step 8: Evaluate application readiness
        WorkdayApplicationReadinessResult readinessResult =
                workdayApplicationReadinessHandler.evaluate(
                        applicationData,
                        formInspectionResult,
                        radioGroupInspections,
                        radioGroupSelectionResults
                );

        // Step 9: Click Save and Continue and verify the transition
        WorkdaySaveAndContinueResult saveAndContinueResult;

        if (readinessResult.readyToProceed()) {

            saveAndContinueResult =
                    workdaySaveAndContinueHandler.clickAndVerify(page);

        } else {

            saveAndContinueResult =
                    new WorkdaySaveAndContinueResult(
                            false,
                            false,
                            "SKIPPED_NOT_READY",
                            page.url(),
                            page.url(),
                            List.of(
                                    "Save and Continue was skipped because "
                                            + "the application is not ready."
                            )
                    );
        }

        return new WorkdayMyInformationStepResult(
                sourceSelectionResult,
                myInformationFillResult,
                formInspectionResult,
                radioGroupInspections,
                radioGroupResolutions,
                radioGroupSelectionResults,
                applicationData,
                readinessResult,
                saveAndContinueResult
        );
    }

}
