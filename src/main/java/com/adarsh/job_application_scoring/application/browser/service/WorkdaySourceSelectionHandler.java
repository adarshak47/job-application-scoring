
package com.adarsh.job_application_scoring.application.browser.service;

import com.adarsh.job_application_scoring.application.browser.dto.WorkdaySourceSelectionResult;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class WorkdaySourceSelectionHandler {

    private static final String SOURCE_FIELD =
            "[data-automation-id='formField-source']";

    private static final String SOURCE_INPUT =
            "#source--source";

    private static final String SELECTED_ITEM =
            "[data-automation-id='selectedItem']";

    private static final String SOURCE_VALUE = "LinkedIn";

    public WorkdaySourceSelectionResult selectSource(Page page) {

        List<String> detectedOptions = new ArrayList<>();

        try {
            // Step 1: Locate the source field.
            Locator sourceField = page.locator(SOURCE_FIELD);

            if (sourceField.count() == 0 || !sourceField.isVisible()) {
                return failure("SOURCE_FIELD_NOT_FOUND", detectedOptions);
            }

            // Step 2: Locate its input.
            Locator sourceInput = sourceField.locator(SOURCE_INPUT);

            if (sourceInput.count() == 0 || !sourceInput.isVisible()) {
                return failure("SOURCE_INPUT_NOT_FOUND", detectedOptions);
            }

            // Step 3: Type LinkedIn into the multi-select.
            sourceInput.click();
            sourceInput.fill(SOURCE_VALUE);

            // Step 4: Select the matching value using Enter.
            sourceInput.press("Enter");

            // Step 5: Verify that LinkedIn was selected.
            Locator selectedItems = sourceField.locator(SELECTED_ITEM);

            selectedItems.first().waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(10000)
            );

            boolean selected = false;

            for (int i = 0; i < selectedItems.count(); i++) {

                String selectedText =
                        selectedItems.nth(i).innerText().trim();

                if (!selectedText.isBlank()) {
                    detectedOptions.add(selectedText);
                }

                if (normalize(selectedText).contains("linkedin")) {
                    selected = true;
                    break;
                }
            }

            if (!selected) {
                return failure(
                        "LINKEDIN_SELECTION_NOT_VERIFIED",
                        detectedOptions
                );
            }

            return new WorkdaySourceSelectionResult(
                    true,
                    SOURCE_VALUE,
                    "LINKEDIN_SELECTED",
                    detectedOptions
            );

        } catch (Exception exception) {

            log.error("Failed to select LinkedIn as application source",
                    exception);

            return failure(
                    "SOURCE_SELECTION_FAILED: "
                            + exception.getMessage(),
                    detectedOptions
            );
        }
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private WorkdaySourceSelectionResult failure(
            String status,
            List<String> detectedOptions
    ) {
        return new WorkdaySourceSelectionResult(
                false,
                null,
                status,
                detectedOptions
        );
    }
}
