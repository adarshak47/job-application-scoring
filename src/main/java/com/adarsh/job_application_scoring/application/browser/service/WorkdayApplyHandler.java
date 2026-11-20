package com.adarsh.job_application_scoring.application.browser.service;

import com.adarsh.job_application_scoring.application.browser.dto.WorkdayApplyResult;
import com.adarsh.job_application_scoring.application.browser.model.WorkdayApplyStatus;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class WorkdayApplyHandler {

    private static final String VIEW_APPLICATION_SELECTOR = "button[data-automation-id='viewButton']";

    private static final String CONTINUE_APPLICATION_SELECTOR = "a[data-automation-id='continueButton'][role='button']";

    private static final String APPLY_BUTTON_SELECTOR = "a[data-automation-id='adventureButton'][role='button']";

    public WorkdayApplyResult clickApply(Page page) {

        List<String> signals = new ArrayList<>();

        try {

            Locator viewApplication = page.locator(VIEW_APPLICATION_SELECTOR).first();

            if (viewApplication.count() > 0) {
                signals.add("Existing application detected");

                return new WorkdayApplyResult(WorkdayApplyStatus.VIEW_APPLICATION, signals);
            }

            Locator continueApplication = page.locator(CONTINUE_APPLICATION_SELECTOR).first();

            if (continueApplication.count() > 0) {
                signals.add("Incomplete application detected");

                return new WorkdayApplyResult(WorkdayApplyStatus.CONTINUE_APPLICATION, signals);
            }

            Locator applyButton = page.locator(APPLY_BUTTON_SELECTOR).first();

            if (applyButton.count() == 0) {
                signals.add("Apply button not found");

                return new WorkdayApplyResult(WorkdayApplyStatus.APPLY_BUTTON_NOT_FOUND, signals);
            }

            if (!applyButton.isVisible()) {
                signals.add("Apply button exists but is not visible");

                return new WorkdayApplyResult(WorkdayApplyStatus.APPLY_BUTTON_NOT_VISIBLE, signals);
            }

            applyButton.click();

            signals.add("Apply button clicked successfully");

            try {
                page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
            } catch (PlaywrightException ignored) {
                // Workday may perform an SPA transition.
            }

            page.waitForTimeout(1500);

            signals.add("Resulting page URL: " + page.url());

            return new WorkdayApplyResult(WorkdayApplyStatus.APPLY_CLICKED, signals);

        } catch (PlaywrightException exception) {

            log.warn("Workday Apply operation failed: {}", exception.getMessage());

            signals.add("Apply operation failed: " + exception.getMessage());

            return new WorkdayApplyResult(WorkdayApplyStatus.ERROR, signals);
        }
    }
}
