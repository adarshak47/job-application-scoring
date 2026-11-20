
package com.adarsh.job_application_scoring.application.browser.service;

import com.adarsh.job_application_scoring.application.browser.BrowserSession;
import com.adarsh.job_application_scoring.application.browser.PlaywrightBrowserFactory;
import com.adarsh.job_application_scoring.application.browser.dto.*;
import com.adarsh.job_application_scoring.application.browser.model.WorkdayApplyOptionsStatus;
import com.adarsh.job_application_scoring.application.browser.model.WorkdayApplyStatus;
import com.adarsh.job_application_scoring.application.browser.model.WorkdaySignInStatus;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class WorkdayApplicationBrowserService {


    private final PlaywrightBrowserFactory browserFactory;

    private final WorkdayApplyHandler workdayApplyHandler;

    private final WorkdayLoginHandler workdayLoginHandler;

    private final WorkdayApplyOptionsHandler workdayApplyOptionsHandler;

    private final WorkdaySignInHandler workdaySignInHandler;

    private final WorkdayMyInformationStepHandler workdayMyInformationStepHandler;

    private final WorkdayMyExperienceStepHandler workdayMyExperienceStepHandler;

    private final WorkdayPageRenderWaitHandler pageRenderWaitHandler;

    private final WorkdayApplicationStepInspectionHandler workdayApplicationStepInspectionHandler;

    private final WorkdayApplicationQuestionsStepHandler workdayApplicationQuestionsStepHandler;

    private final WorkdayVoluntaryDisclosuresStepHandler workdayVoluntaryDisclosuresStepHandler;

    public WorkdayBrowserResponse inspectApplicationPage(
            String applicationUrl) {

        validateWorkdayUrl(applicationUrl);

        try (BrowserSession session = browserFactory.openSession()) {

            Page page = session.getPage();

            Response response = page.navigate(
                    applicationUrl,
                    new Page.NavigateOptions()
                            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            );

            Integer httpStatus = response != null
                    ? response.status()
                    : null;

            // Wait until Workday has rendered its page.
            pageRenderWaitHandler.waitForPageRendered(page);

            String jobTitle = extractJobTitle(page);

            return new WorkdayBrowserResponse(
                    applicationUrl,
                    page.url(),
                    jobTitle,
                    httpStatus
            );
        }
    }

    public WorkdayApplicationStateResponse inspectApplicationState(
            String applicationUrl) {

        validateWorkdayUrl(applicationUrl);

        try (BrowserSession session = browserFactory.openSession()) {

            Page page = session.getPage();

            Response response = page.navigate(
                    applicationUrl,
                    new Page.NavigateOptions()
                            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            );

            Integer httpStatus = response != null
                    ? response.status()
                    : null;

            // IMPORTANT: Do not inspect the page before rendering completes.
            pageRenderWaitHandler.waitForPageRendered(page);

            List<String> signals = new ArrayList<>();

            WorkdayApplicationState state =
                    detectApplicationState(page, httpStatus, signals);

            String jobTitle = extractJobTitleIfAvailable(page);

            return new WorkdayApplicationStateResponse(
                    applicationUrl,
                    page.url(),
                    jobTitle,
                    httpStatus,
                    state,
                    List.copyOf(signals)
            );
        }
    }

    private WorkdayApplicationState detectApplicationState(
            Page page,
            Integer httpStatus,
            List<String> signals) {

        String bodyText = getBodyText(page).toLowerCase(Locale.ROOT);

        // 1. Existing application
        if (containsAny(bodyText,
                "you have already applied",
                "you've already applied",
                "application already submitted",
                "already submitted an application")) {

            signals.add("Existing application message detected");
            return WorkdayApplicationState.EXISTING_APPLICATION;
        }

        // 2. Error page
        if ((httpStatus != null && httpStatus >= 400)
                || containsAny(bodyText,
                "something went wrong",
                "page not found",
                "this page is unavailable",
                "we are unable to process your request")) {

            signals.add("Workday error or unavailable page detected");
            return WorkdayApplicationState.ERROR_PAGE;
        }

        // 3. Application form
        boolean hasApplicationSections =
                containsAny(bodyText,
                        "my information",
                        "my experience",
                        "application questions");

        boolean hasApplicationForm =
                isVisible(page, "[data-automation-id='applicationForm']")
                        || isVisible(page, "[data-automation-id='formField']");

        if (hasApplicationSections && hasApplicationForm) {
            signals.add("Application form sections detected");
            return WorkdayApplicationState.APPLICATION_FORM;
        }

        // 4. Login or account creation
        boolean hasSignInButton =
                isVisible(page, "[data-automation-id='signInButton']");

        boolean hasCreateAccount =
                isVisible(page, "[data-automation-id='createAccountLink']");

        boolean hasPasswordField =
                isVisible(page, "input[type='password']");

        if (hasSignInButton || hasCreateAccount || hasPasswordField) {

            if (hasSignInButton) {
                signals.add("Sign-in control detected");
            }

            if (hasCreateAccount) {
                signals.add("Create-account control detected");
            }

            if (hasPasswordField) {
                signals.add("Password field detected");
            }

            return WorkdayApplicationState.LOGIN_OR_CREATE_ACCOUNT;
        }

        // 5. Apply options
        boolean hasApplyButton =
                isVisible(page, "[data-automation-id='applyButton']");

        boolean hasApplyManually =
                isVisible(page, "[data-automation-id='applyManually']");

        boolean hasAutofill =
                isVisible(page, "[data-automation-id='autofill']");

        if (hasApplyButton || hasApplyManually || hasAutofill
                || containsAny(bodyText,
                "apply manually",
                "autofill with resume",
                "use my last application")) {

            if (hasApplyButton) {
                signals.add("Apply button detected");
            }

            if (hasApplyManually) {
                signals.add("Apply manually option detected");
            }

            if (hasAutofill) {
                signals.add("Autofill option detected");
            }

            return WorkdayApplicationState.APPLY_OPTIONS;
        }

        // 6. Job details
        if (extractJobTitleIfAvailable(page) != null) {
            signals.add("Workday job posting header detected");
            return WorkdayApplicationState.JOB_DETAILS;
        }

        signals.add("No recognized application state detected");
        return WorkdayApplicationState.UNKNOWN;
    }

    private String extractJobTitle(Page page) {

        String title = extractJobTitleIfAvailable(page);

        if (title != null) {
            return title;
        }

        throw new IllegalStateException(
                "Unable to extract job title from Workday application page"
        );
    }

    private String extractJobTitleIfAvailable(Page page) {

        String[] selectors = {
                "[data-automation-id='jobPostingHeader']",
                "h2[data-automation-id='jobPostingHeader']"
        };

        for (String selector : selectors) {

            Locator locator = page.locator(selector).first();

            try {
                locator.waitFor(
                        new Locator.WaitForOptions()
                                .setTimeout(3000)
                );

                if (locator.isVisible()) {
                    String title = locator.innerText().trim();

                    if (!title.isEmpty()) {
                        return title;
                    }
                }

            } catch (Exception ignored) {
                // Try the next selector.
            }
        }

        return null;
    }

    private boolean isVisible(Page page, String selector) {

        try {
            Locator locator = page.locator(selector).first();
            return locator.isVisible();
        } catch (Exception ignored) {
            return false;
        }
    }

    private String getBodyText(Page page) {

        try {
            return page.locator("body").innerText();
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean containsAny(String text, String... phrases) {

        for (String phrase : phrases) {
            if (text.contains(phrase)) {
                return true;
            }
        }

        return false;
    }

    private void validateWorkdayUrl(String applicationUrl) {

        URI uri;

        try {
            uri = URI.create(applicationUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid application URL");
        }

        String host = uri.getHost();

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || host == null
                || !isWorkdayCareersDomain(host)
                || uri.getUserInfo() != null
                || (uri.getPort() != -1 && uri.getPort() != 443)) {

            throw new IllegalArgumentException(
                    "Only HTTPS Workday careers URLs are supported"
            );
        }
    }

    private boolean isWorkdayCareersDomain(String host) {

        String normalizedHost = host.toLowerCase(Locale.ROOT);

        return normalizedHost.endsWith(".myworkdayjobs.com")
                || normalizedHost.equals("myworkdayjobs.com");
    }




    public WorkdayApplicationStartResult startApplication(String applicationUrl) {

        validateWorkdayUrl(applicationUrl);

        try (BrowserSession session = browserFactory.openSession()) {

            Page page = session.getPage();

            // Step 1: Open the Workday application page
            page.navigate(
                    applicationUrl,
                    new Page.NavigateOptions()
                            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            );

            pageRenderWaitHandler.waitForPageRendered(page);

            // Step 2: Click Apply
            WorkdayApplyResult applyResult =
                    workdayApplyHandler.clickApply(page);

            if (applyResult.status() != WorkdayApplyStatus.APPLY_CLICKED) {
                return WorkdayApplicationStartResult.builder()
                        .applyResult(applyResult)
                        .finalUrl(page.url())
                        .build();
            }

            // Step 3: Select Apply manually
            WorkdayApplyOptionsResult applyOptionsResult =
                    workdayApplyOptionsHandler.selectApplyManually(page);

            if (applyOptionsResult.status()
                    != WorkdayApplyOptionsStatus.APPLY_MANUALLY_SELECTED) {

                return WorkdayApplicationStartResult.builder()
                        .applyResult(applyResult)
                        .applyOptionsResult(applyOptionsResult)
                        .finalUrl(page.url())
                        .build();
            }

            pageRenderWaitHandler.waitForPageRendered(page);

            // Step 4: Click Sign In
            WorkdaySignInResult signInResult =
                    workdaySignInHandler.clickSignIn(page);

            if (signInResult.status() != WorkdaySignInStatus.SIGN_IN_CLICKED) {
                return WorkdayApplicationStartResult.builder()
                        .applyResult(applyResult)
                        .applyOptionsResult(applyOptionsResult)
                        .signInResult(signInResult)
                        .finalUrl(page.url())
                        .build();
            }

            pageRenderWaitHandler.waitForPageRendered(page);

            // Step 5: Verify login page
            if (!isLoginPage(page)) {
                return WorkdayApplicationStartResult.builder()
                        .applyResult(applyResult)
                        .applyOptionsResult(applyOptionsResult)
                        .signInResult(signInResult)
                        .finalUrl(page.url())
                        .build();
            }

            // Step 6: Login and verify authentication
            WorkdayLoginResult loginResult =
                    workdayLoginHandler.login(page);

            if (loginResult.status()
                    != WorkdayLoginStatus.LOGIN_SUCCESSFULLY_VERIFIED) {

                return WorkdayApplicationStartResult.builder()
                        .applyResult(applyResult)
                        .applyOptionsResult(applyOptionsResult)
                        .signInResult(signInResult)
                        .loginResult(loginResult)
                        .finalUrl(page.url())
                        .build();
            }

            pageRenderWaitHandler.waitForPageRendered(page);

            // Step 7: Process Step 1 - My Information
            WorkdayMyInformationStepResult myInformationStepResult =
                    workdayMyInformationStepHandler.process(page);

            WorkdaySaveAndContinueResult myInformationSaveResult =
                    myInformationStepResult.saveAndContinueResult();

            WorkdayMyExperienceStepResult myExperienceStepResult = null;
            WorkdayApplicationQuestionsStepResult applicationQuestionsStepResult = null;
            WorkdayVoluntaryDisclosuresStepResult voluntaryDisclosuresStepResult = null;

            WorkdayApplicationStepInspectionResult nextStepInspectionResult = null;

            // Step 8: Process Step 2 - My Experience
            // Only after a verified transition from My Information.
            if (myInformationSaveResult != null
                    && myInformationSaveResult.clicked()
                    && myInformationSaveResult.transitionDetected()) {

                myExperienceStepResult =
                        workdayMyExperienceStepHandler.process(page);

                WorkdaySaveAndContinueResult myExperienceSaveResult =
                        myExperienceStepResult.saveAndContinueResult();

                // Step 9: Process Application Questions
                if (myExperienceSaveResult != null
                        && myExperienceSaveResult.clicked()
                        && myExperienceSaveResult.transitionDetected()) {

                    applicationQuestionsStepResult =
                            workdayApplicationQuestionsStepHandler.process(page);

                    WorkdaySaveAndContinueResult applicationQuestionsSaveResult =
                            applicationQuestionsStepResult.saveAndContinueResult();

                    // Step 10: Process Voluntary Disclosures
                    // Only after Application Questions completed and transitioned.
                    if (applicationQuestionsStepResult.successful()
                            && applicationQuestionsSaveResult != null
                            && applicationQuestionsSaveResult.clicked()
                            && applicationQuestionsSaveResult.transitionDetected()) {

                        voluntaryDisclosuresStepResult =
                                workdayVoluntaryDisclosuresStepHandler.process(page);

                        WorkdaySaveAndContinueResult voluntaryDisclosuresSaveResult =
                                voluntaryDisclosuresStepResult.saveAndContinueResult();

                        // Step 11: Inspect the page after Voluntary Disclosures.
                        if (voluntaryDisclosuresStepResult.successful()
                                && voluntaryDisclosuresSaveResult != null
                                && voluntaryDisclosuresSaveResult.clicked()
                                && voluntaryDisclosuresSaveResult.transitionDetected()) {

                            nextStepInspectionResult =
                                    workdayApplicationStepInspectionHandler.inspect(page);
                        }
                    }
                }
            }

            // Step 12: Return orchestration result
            return WorkdayApplicationStartResult.builder()
                    .applyResult(applyResult)
                    .applyOptionsResult(applyOptionsResult)
                    .signInResult(signInResult)
                    .loginResult(loginResult)
                    .myInformationStepResult(myInformationStepResult)
                    .myExperienceStepResult(myExperienceStepResult)
                    .applicationQuestionsStepResult(applicationQuestionsStepResult)
                    .voluntaryDisclosuresStepResult(voluntaryDisclosuresStepResult)
                    .finalUrl(page.url())
                    .nextStepInspectionResult(nextStepInspectionResult)
                    .build();
        }
    }



    private boolean isLoginPage(Page page) {

        return page.locator("[data-automation-id='signInButton']").count() > 0
                || page.locator("input[type='password']").count() > 0;
    }

}
