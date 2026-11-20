
package com.adarsh.job_application_scoring.application.browser.service;

import com.adarsh.job_application_scoring.application.browser.dto.WorkdaySaveAndContinueResult;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkdaySaveAndContinueHandler {

    private static final String SAVE_AND_CONTINUE_SELECTOR =
            "button[data-automation-id='pageFooterNextButton']";

    private static final int TRANSITION_TIMEOUT_MS = 10_000;
    private static final int POLLING_INTERVAL_MS = 500;

    private final WorkdayPageRenderWaitHandler pageRenderWaitHandler;

    private String capturePageSnapshot(Page page) {
        try {
            Locator applicationPage =
                    page.locator("[data-automation-id='applyFlowPage']").first();

            String content = applicationPage.count() > 0
                    ? applicationPage.innerText()
                    : page.locator("body").innerText();

            return normalize(content);

        } catch (PlaywrightException exception) {
            log.warn("Unable to capture application page snapshot: {}",
                    exception.getMessage());
            return "";
        }
    }

    private WorkdaySaveAndContinueResult click(Page page) {

        List<String> signals = new ArrayList<>();
        String previousUrl = page.url();

        try {
            Locator nextButton =
                    page.locator(SAVE_AND_CONTINUE_SELECTOR).first();

            if (nextButton.count() == 0) {
                signals.add("Save and Continue button not found");

                return result(false, false, "BUTTON_NOT_FOUND",
                        previousUrl, page.url(), signals);
            }

            if (!nextButton.isVisible()) {
                signals.add("Save and Continue button is not visible");

                return result(false, false, "BUTTON_NOT_VISIBLE",
                        previousUrl, page.url(), signals);
            }

            if (!nextButton.isEnabled()) {
                signals.add("Save and Continue button is disabled");

                return result(false, false, "BUTTON_DISABLED",
                        previousUrl, page.url(), signals);
            }

            nextButton.click();

            signals.add("Save and Continue clicked successfully");

            return result(true, false, "CLICKED",
                    previousUrl, page.url(), signals);

        } catch (PlaywrightException exception) {
            log.warn("Unable to click Save and Continue: {}",
                    exception.getMessage());

            signals.add("Save and Continue click failed: "
                    + exception.getMessage());

            return result(false, false, "CLICK_FAILED",
                    previousUrl, page.url(), signals);
        }
    }

    private WorkdaySaveAndContinueResult verifyTransition(
            Page page,
            WorkdaySaveAndContinueResult clickResult,
            String previousPageSnapshot
    ) {

        if (clickResult == null || !clickResult.clicked()) {
            return clickResult;
        }

        List<String> signals =
                new ArrayList<>(clickResult.detectedSignals());

        long startTime = System.nanoTime();

        try {
            while ((System.nanoTime() - startTime)
                    / 1_000_000 < TRANSITION_TIMEOUT_MS) {

                String currentUrl = page.url();
                String currentSnapshot = capturePageSnapshot(page);

                if (!currentUrl.equals(clickResult.previousUrl())) {
                    signals.add("Page URL changed after Save and Continue");

                    return result(true, true, "TRANSITION_VERIFIED",
                            clickResult.previousUrl(), currentUrl, signals);
                }

                if (!currentSnapshot.isBlank()
                        && !previousPageSnapshot.isBlank()
                        && !currentSnapshot.equals(previousPageSnapshot)) {

                    signals.add("Application page content changed after Save and Continue");

                    return result(true, true, "TRANSITION_VERIFIED",
                            clickResult.previousUrl(), currentUrl, signals);
                }

                Thread.sleep(POLLING_INTERVAL_MS);
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            signals.add("Transition verification interrupted");

            return result(true, false, "VERIFICATION_INTERRUPTED",
                    clickResult.previousUrl(), page.url(), signals);

        } catch (PlaywrightException exception) {
            signals.add("Transition verification failed: "
                    + exception.getMessage());

            return result(true, false, "VERIFICATION_FAILED",
                    clickResult.previousUrl(), page.url(), signals);
        }

        signals.add("No page URL or content transition detected");

        return result(true, false, "TRANSITION_NOT_DETECTED",
                clickResult.previousUrl(), page.url(), signals);
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private WorkdaySaveAndContinueResult result(
            boolean clicked,
            boolean transitionDetected,
            String status,
            String previousUrl,
            String resultingUrl,
            List<String> signals
    ) {
        return new WorkdaySaveAndContinueResult(
                clicked,
                transitionDetected,
                status,
                previousUrl,
                resultingUrl,
                List.copyOf(signals)
        );
    }

    public WorkdaySaveAndContinueResult clickAndVerify(Page page) {

        // Capture the current page before clicking.
        String previousPageSnapshot = capturePageSnapshot(page);

        // Click Save and Continue.
        WorkdaySaveAndContinueResult result = click(page);

        // Stop if the button could not be clicked.
        if (!result.clicked()) {
            return result;
        }

        // Wait for the resulting page to render.
        pageRenderWaitHandler.waitForPageRendered(page);

        // Verify that the application page actually transitioned.
        return verifyTransition(
                page,
                result,
                previousPageSnapshot
        );
    }
}
