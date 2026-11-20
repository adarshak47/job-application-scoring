
package com.adarsh.job_application_scoring.application.browser.dto;

import com.adarsh.job_application_scoring.application.data.dto.WorkdayApplicationData;

import java.util.List;

public record WorkdayMyInformationStepResult(

        WorkdaySourceSelectionResult sourceSelectionResult,

        WorkdayMyInformationFillResult myInformationFillResult,

        WorkdayApplicationStepInspectionResult formInspectionResult,

        List<WorkdayRadioGroupInspection> radioGroupInspections,

        List<WorkdayRadioGroupResolution> radioGroupResolutions,

        List<WorkdayRadioGroupSelectionResult> radioGroupSelectionResults,

        WorkdayApplicationData applicationData,

        WorkdayApplicationReadinessResult readinessResult,

        WorkdaySaveAndContinueResult saveAndContinueResult

) {
}
