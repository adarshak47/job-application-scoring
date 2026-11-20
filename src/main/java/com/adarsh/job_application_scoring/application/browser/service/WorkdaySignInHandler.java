package com.adarsh.job_application_scoring.application.browser.service;

import com.adarsh.job_application_scoring.application.browser.dto.WorkdaySignInResult;
import com.adarsh.job_application_scoring.application.browser.model.WorkdaySignInStatus;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class WorkdaySignInHandler {

    private static final String SIGN_IN_SELECTOR =
            "button[data-automation-id='signInLink']";

    public WorkdaySignInResult clickSignIn(Page page) {

        List<String> signals = new ArrayList<>();

        try {
            Locator signInButton = page.locator(SIGN_IN_SELECTOR).first();

            if (signInButton.count() == 0) {
                signals.add("Sign In button not found");

                return result(
                        WorkdaySignInStatus.SIGN_IN_LINK_NOT_FOUND,
                        signals
                );
            }

            if (!signInButton.isVisible()) {
                signals.add("Sign In button is not visible");

                return result(
                        WorkdaySignInStatus.SIGN_IN_LINK_NOT_VISIBLE,
                        signals
                );
            }

            signInButton.click();

            signals.add("Sign In button clicked successfully");

            return result(
                    WorkdaySignInStatus.SIGN_IN_CLICKED,
                    signals
            );

        } catch (PlaywrightException exception) {

            log.warn("Unable to click Workday Sign In button: {}",
                    exception.getMessage());

            signals.add("Sign In click failed: " + exception.getMessage());

            return result(
                    WorkdaySignInStatus.SIGN_IN_FAILED,
                    signals
            );
        }
    }

    private WorkdaySignInResult result(
            WorkdaySignInStatus status,
            List<String> signals
    ) {
        return new WorkdaySignInResult(status, signals);
    }
}