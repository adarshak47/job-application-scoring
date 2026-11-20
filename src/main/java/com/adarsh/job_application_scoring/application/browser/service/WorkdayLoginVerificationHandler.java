
package com.adarsh.job_application_scoring.application.browser.service;

import com.adarsh.job_application_scoring.application.browser.dto.WorkdayLoginVerificationResult;
import com.adarsh.job_application_scoring.application.browser.model.WorkdayLoginVerificationStatus;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class WorkdayLoginVerificationHandler {

    public WorkdayLoginVerificationResult verify(Page page) {

        List<String> signals = new ArrayList<>();

        try {

            // Step 1: Check CAPTCHA.
            if (isCaptchaPresent(page)) {

                signals.add("CAPTCHA detected after login submission");

                return result(
                        WorkdayLoginVerificationStatus.CAPTCHA_REQUIRED,
                        page,
                        signals
                );
            }

            // Step 2: Check MFA.
            if (isMfaRequired(page)) {

                signals.add("MFA detected after login submission");

                return result(
                        WorkdayLoginVerificationStatus.MFA_REQUIRED,
                        page,
                        signals
                );
            }

            // Step 3: Check invalid credentials.
            if (isInvalidCredentials(page)) {

                signals.add("Invalid credentials message detected");

                return result(
                        WorkdayLoginVerificationStatus.INVALID_CREDENTIALS,
                        page,
                        signals
                );
            }

            // Step 4: Check whether login form is still visible.
            if (isLoginFormVisible(page)) {

                signals.add("Login form is still visible");

                return result(
                        WorkdayLoginVerificationStatus.LOGIN_FORM_STILL_VISIBLE,
                        page,
                        signals
                );
            }

            // Step 5: Check whether Workday Apply Flow is visible.
            if (isApplicationFormVisible(page)) {

                signals.add("Workday Apply Flow detected");

                signals.add(
                        "My Information application page detected"
                );

                signals.add(
                        "Application form fields are visible"
                );

                signals.add(
                        "Login successfully verified"
                );

                return result(
                        WorkdayLoginVerificationStatus.LOGIN_SUCCESSFULLY_VERIFIED,
                        page,
                        signals
                );
            }

            // Step 6: Unknown page.
            signals.add(
                    "Login form disappeared, but Workday application page was not identified"
            );

            return result(
                    WorkdayLoginVerificationStatus.AUTHENTICATION_NOT_VERIFIED,
                    page,
                    signals
            );

        } catch (Exception exception) {

            log.warn(
                    "Unable to verify Workday login: {}",
                    exception.getMessage()
            );

            signals.add(
                    "Login verification failed: " + exception.getMessage()
            );

            return result(
                    WorkdayLoginVerificationStatus.AUTHENTICATION_NOT_VERIFIED,
                    page,
                    signals
            );
        }
    }

    /**
     * Checks whether the login form is still visible.
     */
    private boolean isLoginFormVisible(Page page) {

        Locator passwordField = page.locator(
                "input[type='password']"
        );

        Locator emailField = page.locator(
                "input[type='email'], " +
                        "input[name='username'], " +
                        "[data-automation-id='email']"
        );

        return hasVisibleElement(passwordField)
                && hasVisibleElement(emailField);
    }

    /**
     * Identifies the Workday application form.
     *
     * Uses actual automation IDs and element IDs
     * observed in the supplied Workday DOM.
     */
    private boolean isApplicationFormVisible(Page page) {

        // Step 1: Verify Apply Flow container.
        Locator applyFlow = page.locator(
                "[data-automation-id='applyFlowPage']"
        );

        if (!hasVisibleElement(applyFlow)) {
            return false;
        }

        // Step 2: Verify My Information page.
        Locator myInfoPage = page.locator(
                "[data-automation-id='applyFlowMyInfoPage']"
        );

        if (!hasVisibleElement(myInfoPage)) {
            return false;
        }

        // Step 3: Verify application progress bar.
        Locator progressBar = page.locator(
                "[data-automation-id='progressBar']"
        );

        if (!hasVisibleElement(progressBar)) {
            return false;
        }

        // Step 4: Verify active application step.
        Locator activeStep = page.locator(
                "[data-automation-id='progressBarActiveStep']"
        );

        if (!hasVisibleElement(activeStep)) {
            return false;
        }

        // Step 5: Verify actual application fields.
        Locator firstNameField = page.locator(
                "#name--legalName--firstName"
        );

        Locator lastNameField = page.locator(
                "#name--legalName--lastName"
        );

        Locator addressField = page.locator(
                "#address--addressLine1"
        );

        boolean hasPersonalInformationFields =
                hasVisibleElement(firstNameField)
                        && hasVisibleElement(lastNameField);

        boolean hasAddressField =
                hasVisibleElement(addressField);

        // Step 6: Verify Save and Continue button.
        Locator continueButton = page.locator(
                "[data-automation-id='pageFooterNextButton']"
        );

        boolean hasContinueButton =
                hasVisibleElement(continueButton);

        return hasPersonalInformationFields
                && hasAddressField
                && hasContinueButton;
    }

    /**
     * Checks whether at least one matching element is visible.
     */
    private boolean hasVisibleElement(Locator locator) {

        for (int i = 0; i < locator.count(); i++) {

            Locator element = locator.nth(i);

            if (element.isVisible()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks for CAPTCHA.
     */
    private boolean isCaptchaPresent(Page page) {

        Locator captcha = page.locator(
                "iframe[src*='recaptcha'], " +
                        "iframe[src*='hcaptcha'], " +
                        "[data-sitekey]"
        );

        return hasVisibleElement(captcha);
    }

    /**
     * Checks for MFA.
     */
    private boolean isMfaRequired(Page page) {

        Locator verificationCodeField = page.locator(
                "input[autocomplete='one-time-code'], " +
                        "[data-automation-id='verificationCode']"
        );

        if (hasVisibleElement(verificationCodeField)) {
            return true;
        }

        String pageText = page.locator("body")
                .innerText()
                .toLowerCase(Locale.ROOT);

        return pageText.contains("verification code")
                || pageText.contains("two-factor authentication")
                || pageText.contains("multi-factor authentication");
    }

    /**
     * Checks for invalid credentials.
     */
    private boolean isInvalidCredentials(Page page) {

        String pageText = page.locator("body")
                .innerText()
                .toLowerCase(Locale.ROOT);

        return pageText.contains("invalid username or password")
                || pageText.contains("incorrect username or password")
                || pageText.contains("invalid credentials")
                || pageText.contains("incorrect password");
    }

    private WorkdayLoginVerificationResult result(
            WorkdayLoginVerificationStatus status,
            Page page,
            List<String> signals
    ) {

        return new WorkdayLoginVerificationResult(
                status,
                page.url(),
                signals
        );
    }
}
