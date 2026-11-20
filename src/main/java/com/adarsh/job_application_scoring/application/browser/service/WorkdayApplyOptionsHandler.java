
package com.adarsh.job_application_scoring.application.browser.service;

import com.adarsh.job_application_scoring.application.browser.dto.WorkdayApplyOptionsResult;
import com.adarsh.job_application_scoring.application.browser.model.WorkdayApplyOptionsStatus;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class WorkdayApplyOptionsHandler {

    private static final String DIALOG_SELECTOR =
            "[data-automation-id='wd-popup-frame'][role='dialog']";

    private static final String APPLY_MANUALLY_SELECTOR =
            "[data-automation-id='applyManually']";

    public WorkdayApplyOptionsResult selectApplyManually(Page page) {

        List<String> signals = new ArrayList<>();

        Locator dialog = page.locator(DIALOG_SELECTOR).first();

        if (dialog.count() == 0 || !dialog.isVisible()) {

            signals.add("Application options dialog not found");

            return result(
                    WorkdayApplyOptionsStatus.OPTIONS_DIALOG_NOT_PRESENT,
                    signals
            );
        }

        Locator applyManuallyOption =
                dialog.locator(APPLY_MANUALLY_SELECTOR).first();

        if (applyManuallyOption.count() == 0) {

            signals.add("Apply manually option not found");

            return result(
                    WorkdayApplyOptionsStatus.APPLY_MANUALLY_OPTION_NOT_FOUND,
                    signals
            );
        }

        try {

            if (!applyManuallyOption.isVisible()) {

                signals.add("Apply manually option is not visible");

                return result(
                        WorkdayApplyOptionsStatus.APPLY_MANUALLY_OPTION_NOT_VISIBLE,
                        signals
                );
            }

            applyManuallyOption.click();

            signals.add("Apply manually option selected");

            return result(
                    WorkdayApplyOptionsStatus.APPLY_MANUALLY_SELECTED,
                    signals
            );

        } catch (PlaywrightException exception) {

            log.warn("Unable to select Apply manually: {}",
                    exception.getMessage());

            signals.add("Selection failed: " + exception.getMessage());

            return result(
                    WorkdayApplyOptionsStatus.SELECTION_FAILED,
                    signals
            );
        }
    }

    private WorkdayApplyOptionsResult result(
            WorkdayApplyOptionsStatus status,
            List<String> signals
    ) {
        return new WorkdayApplyOptionsResult(status, signals);
    }
}
