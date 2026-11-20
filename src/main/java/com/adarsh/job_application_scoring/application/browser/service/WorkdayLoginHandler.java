
package com.adarsh.job_application_scoring.application.browser.service;

import com.adarsh.job_application_scoring.application.browser.config.WorkdayLoginProperties;
import com.adarsh.job_application_scoring.application.browser.dto.WorkdayLoginResult;
import com.adarsh.job_application_scoring.application.browser.dto.WorkdayLoginStatus;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class WorkdayLoginHandler {

    private static final int NETWORK_IDLE_TIMEOUT_MS = 5000;
    private static final int PAGE_RENDER_TIMEOUT_MS = 20000;
    private static final int DOM_STABILITY_PERIOD_MS = 1500;

    private static final String LOGIN_FORM_SELECTOR =
            "form[data-automation-id='signInFormo']";

    private static final String EMAIL_SELECTOR =
            "input[data-automation-id='email']";

    private static final String PASSWORD_SELECTOR =
            "input[data-automation-id='password']";

    private static final String SIGN_IN_SELECTOR =
            "div[data-automation-id='click_filter'][role='button'][aria-label='Sign In']";

    private final WorkdayLoginProperties properties;

    public WorkdayLoginHandler(WorkdayLoginProperties properties) {
        this.properties = properties;
    }

    public WorkdayLoginResult login(Page page) {

        List<String> signals = new ArrayList<>();

        // Step 1: Validate credentials
        if (!hasText(properties.getEmail())
                || !hasText(properties.getPassword())) {

            signals.add("Workday login credentials are not configured");

            return result(
                    WorkdayLoginStatus.CREDENTIALS_NOT_CONFIGURED,
                    signals
            );
        }

        try {

            // Step 2: Locate login form
            Locator loginForm = page.locator(LOGIN_FORM_SELECTOR).first();

            if (loginForm.count() == 0 || !loginForm.isVisible()) {

                signals.add("Workday login form not found");

                return result(
                        WorkdayLoginStatus.LOGIN_FORM_NOT_FOUND,
                        signals
                );
            }

            // Step 3: Locate credential fields
            Locator emailField = loginForm.locator(EMAIL_SELECTOR).first();
            Locator passwordField = loginForm.locator(PASSWORD_SELECTOR).first();

            if (emailField.count() == 0 || passwordField.count() == 0
                    || !emailField.isVisible() || !passwordField.isVisible()) {

                signals.add("Login credential fields not found");

                return result(
                        WorkdayLoginStatus.LOGIN_FORM_NOT_FOUND,
                        signals
                );
            }

            // Step 4: Enter credentials
            emailField.fill(properties.getEmail());
            passwordField.fill(properties.getPassword());

            // Step 5: Locate Sign In control
            Locator signInButton = loginForm.locator(SIGN_IN_SELECTOR).first();

            signInButton.waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
            );

            if (!signInButton.isEnabled()) {

                signals.add("Sign In control is not enabled");

                return result(
                        WorkdayLoginStatus.LOGIN_FAILED,
                        signals
                );
            }

            // Step 6: Submit login
            signInButton.click();

            signals.add("Login form submitted");

            // Step 7: Wait for login processing
            waitForPageRendered(page);

            // Step 8: Check invalid credentials
            if (isInvalidCredentials(page)) {

                signals.add("Invalid login credentials detected");

                return result(
                        WorkdayLoginStatus.INVALID_CREDENTIALS,
                        signals
                );
            }

            // Step 9: Check whether login form is still visible
            if (isLoginFormVisible(page)) {

                signals.add("Login form is still visible");

                return result(
                        WorkdayLoginStatus.LOGIN_FORM_STILL_VISIBLE,
                        signals
                );
            }

            // Step 10: Verify application form
            if (isApplicationFormVisible(page)) {

                signals.add("Workday application form detected");
                signals.add("Login successfully verified");

                return result(
                        WorkdayLoginStatus.LOGIN_SUCCESSFULLY_VERIFIED,
                        signals
                );
            }

            // Step 11: Unknown page
            signals.add(
                    "Login form disappeared, but authentication could not be verified"
            );

            return result(
                    WorkdayLoginStatus.AUTHENTICATION_NOT_VERIFIED,
                    signals
            );

        } catch (TimeoutError exception) {

            log.warn("Timed out during Workday login: {}",
                    exception.getMessage());

            signals.add("Timed out waiting for login controls");

            return result(
                    WorkdayLoginStatus.LOGIN_FAILED,
                    signals
            );

        } catch (PlaywrightException exception) {

            log.warn("Workday login failed: {}", exception.getMessage());

            signals.add("Login operation failed: " + exception.getMessage());

            return result(
                    WorkdayLoginStatus.LOGIN_FAILED,
                    signals
            );
        }
    }

    private boolean isLoginFormVisible(Page page) {

        Locator loginForm = page.locator(LOGIN_FORM_SELECTOR);

        return hasVisibleElement(loginForm);
    }
    private boolean isApplicationFormVisible(Page page) {

        Locator progressBar = page.locator(
                "[data-automation-id='progressBar']"
        );

        Locator activeStep = page.locator(
                "[data-automation-id='progressBarActiveStep']"
        );

        return hasVisibleElement(progressBar)
                && hasVisibleElement(activeStep);
    }

    private boolean isInvalidCredentials(Page page) {

        String bodyText = page.locator("body")
                .innerText()
                .toLowerCase(Locale.ROOT);

        return bodyText.contains("incorrect username or password")
                || bodyText.contains("invalid username or password")
                || bodyText.contains("invalid credentials")
                || bodyText.contains("password is incorrect");
    }

    private boolean hasVisibleElement(Locator locator) {

        for (int i = 0; i < locator.count(); i++) {

            if (locator.nth(i).isVisible()) {
                return true;
            }
        }

        return false;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private WorkdayLoginResult result(
            WorkdayLoginStatus status,
            List<String> signals
    ) {
        return new WorkdayLoginResult(status, List.copyOf(signals));
    }

    /**
     * Waits for the initial document load, gives network requests
     * an opportunity to finish, and then waits for DOM stability.
     */
    private void waitForPageRendered(Page page) {

        // Step 1: Wait for the initial document to be parsed.
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Step 2: Wait for network activity to settle.
        // Workday may maintain background requests, so this is best-effort.
        try {
            page.waitForLoadState(
                    LoadState.NETWORKIDLE,
                    new Page.WaitForLoadStateOptions()
                            .setTimeout(NETWORK_IDLE_TIMEOUT_MS)
            );
        } catch (Exception ignored) {
            // Continue with DOM-based synchronization.
        }

        // Step 3: Wait until the document is completely loaded
        // and meaningful content remains stable.
        try {

            page.waitForFunction(
                    "() => new Promise(resolve => {" +

                            "  const quietPeriod = " + DOM_STABILITY_PERIOD_MS + ";" +

                            "  let timer = null;" +

                            "  const isReady = () => {" +
                            "    const body = document.body;" +
                            "    if (!body || document.readyState !== 'complete') {" +
                            "      return false;" +
                            "    }" +

                            "    const text = (body.innerText || '').trim();" +
                            "    const hasControls = body.querySelector(" +
                            "      'input, button, select, textarea, " +
                            "       [data-automation-id], h1, h2'" +
                            "    ) !== null;" +

                            "    return text.length > 0 || hasControls;" +
                            "  };" +

                            "  const observer = new MutationObserver(() => {" +
                            "    checkReadiness();" +
                            "  });" +

                            "  const checkReadiness = () => {" +
                            "    if (!isReady()) {" +
                            "      if (timer !== null) {" +
                            "        clearTimeout(timer);" +
                            "        timer = null;" +
                            "      }" +
                            "      return;" +
                            "    }" +

                            "    if (timer !== null) {" +
                            "      clearTimeout(timer);" +
                            "    }" +

                            "    timer = setTimeout(() => {" +
                            "      observer.disconnect();" +
                            "      resolve(true);" +
                            "    }, quietPeriod);" +
                            "  };" +

                            "  observer.observe(document.documentElement, {" +
                            "    childList: true," +
                            "    subtree: true," +
                            "    attributes: true," +
                            "    characterData: true" +
                            "  });" +

                            "  checkReadiness();" +
                            "})",

                    null,

                    new Page.WaitForFunctionOptions()
                            .setTimeout(PAGE_RENDER_TIMEOUT_MS)
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Workday page did not finish rendering within "
                            + PAGE_RENDER_TIMEOUT_MS + " ms",
                    e
            );
        }
    }

}
