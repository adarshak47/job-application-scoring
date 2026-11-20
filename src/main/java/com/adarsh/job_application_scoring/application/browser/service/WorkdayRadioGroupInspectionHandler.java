
package com.adarsh.job_application_scoring.application.browser.service;

import com.adarsh.job_application_scoring.application.browser.dto.WorkdayRadioGroupInspection;
import com.adarsh.job_application_scoring.application.browser.dto.WorkdayRadioOptionInspection;
import com.adarsh.job_application_scoring.application.question.dto.ApplicationQuestionFieldIdentifier;
import com.adarsh.job_application_scoring.application.question.model.ApplicationQuestionFieldIdentifierType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WorkdayRadioGroupInspectionHandler {

    private static final String FORM_SELECTOR =
            "[data-automation-id='applyFlowMyInfoPage']";

    public List<WorkdayRadioGroupInspection> inspect(Page page) {

        Locator form = page.locator(FORM_SELECTOR);

        if (form.count() == 0 || !form.isVisible()) {
            return List.of();
        }

        Locator fieldWrappers =
                form.locator("[data-automation-id^='formField-']");

        List<WorkdayRadioGroupInspection> results = new ArrayList<>();

        for (int i = 0; i < fieldWrappers.count(); i++) {

            Locator wrapper = fieldWrappers.nth(i);

            Locator radioInputs =
                    wrapper.locator("input[type='radio']");

            if (radioInputs.count() == 0) {
                continue;
            }

            List<ApplicationQuestionFieldIdentifier> identifiers =
                    extractIdentifiers(wrapper, radioInputs);

            List<WorkdayRadioOptionInspection> options =
                    extractOptions(radioInputs);

            String label = extractQuestionLabel(wrapper);

            boolean required =
                    wrapper.locator("[aria-required='true']").count() > 0
                            || wrapper.locator("[required]").count() > 0
                            || wrapper.locator("abbr[aria-hidden='true']").count() > 0;

            results.add(new WorkdayRadioGroupInspection(
                    label,
                    required,
                    identifiers,
                    options
            ));
        }

        return results;
    }

    private List<ApplicationQuestionFieldIdentifier> extractIdentifiers(
            Locator wrapper,
            Locator radioInputs
    ) {

        List<ApplicationQuestionFieldIdentifier> identifiers =
                new ArrayList<>();

        addIdentifier(
                identifiers,
                ApplicationQuestionFieldIdentifierType.AUTOMATION_ID,
                wrapper.getAttribute("data-automation-id")
        );

        addIdentifier(
                identifiers,
                ApplicationQuestionFieldIdentifierType.FKIT_ID,
                wrapper.getAttribute("data-fkit-id")
        );

        addIdentifier(
                identifiers,
                ApplicationQuestionFieldIdentifierType.FIELD_NAME,
                radioInputs.first().getAttribute("name")
        );

        return identifiers;
    }

    private void addIdentifier(
            List<ApplicationQuestionFieldIdentifier> identifiers,
            ApplicationQuestionFieldIdentifierType type,
            String value
    ) {

        if (value == null || value.isBlank()) {
            return;
        }

        identifiers.add(new ApplicationQuestionFieldIdentifier(
                type,
                value.trim()
        ));
    }


    private List<WorkdayRadioOptionInspection> extractOptions(
            Locator radioInputs
    ) {

        List<WorkdayRadioOptionInspection> options = new ArrayList<>();

        for (int i = 0; i < radioInputs.count(); i++) {

            Locator radio = radioInputs.nth(i);

            String value = radio.getAttribute("value");

            String label = "";

            try {
                // The option label is a sibling of the radio's parent.
                Locator optionLabel =
                        radio.locator("xpath=../..").locator("label");

                if (optionLabel.count() > 0) {
                    label = optionLabel.first().innerText().trim();
                }

            } catch (Exception ignored) {
                // Keep the label empty if it cannot be extracted.
            }

            options.add(new WorkdayRadioOptionInspection(
                    value,
                    label,
                    radio.isChecked()
            ));
        }

        return options;
    }

    private String extractQuestionLabel(Locator wrapper) {

        Locator legend = wrapper.locator("fieldset legend");

        if (legend.count() == 0) {
            return null;
        }

        String label = legend.first().innerText();

        return label == null ? null : label.trim();
    }
}
