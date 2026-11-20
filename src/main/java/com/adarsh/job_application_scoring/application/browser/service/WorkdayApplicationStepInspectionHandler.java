
package com.adarsh.job_application_scoring.application.browser.service;

import com.adarsh.job_application_scoring.application.browser.dto.WorkdayApplicationStepInspectionResult;
import com.adarsh.job_application_scoring.application.browser.dto.WorkdayFormFieldInspection;
import com.adarsh.job_application_scoring.application.browser.dto.WorkdayFormOptionInspection;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class WorkdayApplicationStepInspectionHandler {

    public WorkdayApplicationStepInspectionResult inspect(Page page) {

        List<String> signals = new ArrayList<>();

        Locator stepRoot = findApplicationStepRoot(page);

        boolean applicationStepDetected =
                !stepRoot.evaluate("element => element.tagName")
                        .toString()
                        .equalsIgnoreCase("BODY");

        String stepAutomationId = getAttribute(
                stepRoot,
                "data-automation-id"
        );

        List<String> sectionHeadings =
                extractSectionHeadings(stepRoot);

        List<WorkdayFormFieldInspection> fields =
                extractFields(page, stepRoot);

        List<String> visibleButtons =
                extractVisibleButtons(stepRoot);

        if (applicationStepDetected) {
            signals.add("APPLICATION_STEP_DETECTED");
        }

        if (!stepAutomationId.isBlank()) {
            signals.add("STEP_AUTOMATION_ID_FOUND");
        }

        if (!sectionHeadings.isEmpty()) {
            signals.add("SECTION_HEADINGS_FOUND");
        }

        if (!fields.isEmpty()) {
            signals.add("FORM_FIELDS_FOUND");
        }

        if (!visibleButtons.isEmpty()) {
            signals.add("VISIBLE_BUTTONS_FOUND");
        }

        log.info(
                "Workday application step inspection completed. " +
                        "Step: {}, Sections: {}, Fields: {}, Buttons: {}",
                stepAutomationId,
                sectionHeadings.size(),
                fields.size(),
                visibleButtons.size()
        );

        return new WorkdayApplicationStepInspectionResult(
                applicationStepDetected,
                stepAutomationId,
                page.url(),
                sectionHeadings,
                fields,
                visibleButtons,
                signals
        );
    }

    private Locator findApplicationStepRoot(Page page) {

        Locator applicationFlowPage = page.locator(
                "[data-automation-id='applyFlowPage']"
        );

        if (hasVisibleElement(applicationFlowPage)) {
            return applicationFlowPage.first();
        }

        Locator stepPages = page.locator(
                "[data-automation-id^='applyFlow']" +
                        "[data-automation-id$='Page']"
        );

        for (int i = stepPages.count() - 1; i >= 0; i--) {

            Locator stepPage = stepPages.nth(i);

            if (stepPage.isVisible()) {
                return stepPage;
            }
        }

        log.warn(
                "Could not identify a Workday application step container. " +
                        "Inspecting the page body as a fallback."
        );

        return page.locator("body");
    }

    private List<String> extractSectionHeadings(Locator root) {

        List<String> headings = new ArrayList<>();

        Locator locator = root.locator("h1, h2, h3, h4, legend");

        for (int i = 0; i < locator.count(); i++) {

            Locator heading = locator.nth(i);

            if (!heading.isVisible()) {
                continue;
            }

            String text = heading.innerText().trim();

            if (!text.isBlank() && !headings.contains(text)) {
                headings.add(text);
            }
        }

        return headings;
    }

    private List<WorkdayFormFieldInspection> extractFields(
            Page page,
            Locator root
    ) {

        List<WorkdayFormFieldInspection> fields = new ArrayList<>();

        Locator formFields = root.locator(
                "[data-automation-id^='formField-']"
        );

        for (int i = 0; i < formFields.count(); i++) {

            Locator field = formFields.nth(i);

            if (!field.isVisible()) {
                continue;
            }

            WorkdayFormFieldInspection inspection =
                    inspectField(page, field);

            if (inspection != null) {
                fields.add(inspection);
            }
        }

        return fields;
    }

    private WorkdayFormFieldInspection inspectField(
            Page page,
            Locator field
    ) {

        String automationId = getAttribute(
                field,
                "data-automation-id"
        );

        String label = extractLabel(field);

        boolean required = isRequired(field);

        // Radio group
        Locator radios = field.locator("input[type='radio']");

        if (radios.count() > 0) {
            return inspectRadioGroup(
                    page, field, radios, automationId, label, required
            );
        }

        // Multiselect
        Locator multiSelect = field.locator(
                "[data-automation-id='multiSelectContainer']"
        );

        if (multiSelect.count() > 0) {
            return inspectMultiSelect(
                    multiSelect, automationId, label, required
            );
        }

        // Workday dropdown
        Locator dropdown = field.locator(
                "button[aria-haspopup='listbox']"
        );

        if (dropdown.count() > 0) {
            return inspectDropdown(
                    page,
                    field,
                    dropdown.first(),
                    automationId,
                    label,
                    required
            );
        }

        // Checkbox
        Locator checkbox = field.locator(
                "input[type='checkbox']"
        );

        if (checkbox.count() > 0) {
            return inspectCheckbox(
                    checkbox.first(), automationId, label, required
            );
        }

        // File upload
        Locator fileInput = field.locator(
                "input[type='file']"
        );

        if (fileInput.count() > 0) {
            return inspectFileUpload(
                    fileInput.first(), automationId, label, required
            );
        }

        // Text inputs, textareas and native selects
        Locator input = field.locator(
                "input:not([type='hidden']), textarea, select"
        );

        if (input.count() > 0) {
            return inspectInput(
                    input.first(), automationId, label, required
            );
        }

        return null;
    }

    private WorkdayFormFieldInspection inspectRadioGroup(
            Page page,
            Locator field,
            Locator radios,
            String automationId,
            String label,
            boolean required
    ) {

        List<WorkdayFormOptionInspection> options =
                new ArrayList<>();

        String name = "";
        String id = "";
        String selectedValue = "";

        for (int i = 0; i < radios.count(); i++) {

            Locator radio = radios.nth(i);

            String radioId = getAttribute(radio, "id");
            String radioName = getAttribute(radio, "name");
            String value = getAttribute(radio, "value");

            if (i == 0) {
                id = radioId;
                name = radioName;
            }

            String optionText = "";

            if (!radioId.isBlank()) {

                Locator optionLabel = page.locator(
                        "label[for='" + radioId + "']"
                );

                if (optionLabel.count() > 0) {
                    optionText =
                            optionLabel.first().innerText().trim();
                }
            }

            boolean selected = radio.isChecked();

            if (selected) {
                selectedValue = value;
            }

            options.add(new WorkdayFormOptionInspection(
                    optionText,
                    value,
                    selected
            ));
        }

        return new WorkdayFormFieldInspection(
                automationId,
                id,
                name,
                label,
                "INPUT",
                "RADIO",
                selectedValue,
                required,
                !selectedValue.isBlank(),
                List.of(),
                options
        );
    }

    private WorkdayFormFieldInspection inspectMultiSelect(
            Locator multiSelect,
            String automationId,
            String label,
            boolean required
    ) {

        Locator input = multiSelect.locator("input");

        String id = input.count() > 0
                ? getAttribute(input.first(), "id")
                : "";

        String name = input.count() > 0
                ? getAttribute(input.first(), "name")
                : "";

        List<String> selectedValues = new ArrayList<>();

        Locator selectedItems = multiSelect.locator(
                "[data-automation-id='selectedItem']"
        );

        for (int i = 0; i < selectedItems.count(); i++) {

            Locator item = selectedItems.nth(i);

            String value = getAttribute(item, "aria-label");

            if (value.isBlank()) {
                value = item.innerText().trim();
            }

            if (!value.isBlank()) {
                selectedValues.add(value);
            }
        }

        return new WorkdayFormFieldInspection(
                automationId,
                id,
                name,
                label,
                "INPUT",
                "MULTI_SELECT",
                "",
                required,
                false,
                selectedValues,
                List.of()
        );
    }

    private WorkdayFormFieldInspection inspectDropdown(
            Page page,
            Locator field,
            Locator dropdown,
            String automationId,
            String label,
            boolean required
    ) {

        String selectedLabel = dropdown.innerText().trim();
        String selectedValue = getAttribute(dropdown, "value");

        List<WorkdayFormOptionInspection> options = new ArrayList<>();
        List<String> selectedValues = new ArrayList<>();

        try {
            dropdown.click();

            Locator listboxes = page.locator("[role='listbox']:visible");

            if (listboxes.count() > 0) {

                Locator listbox = listboxes.last();

                required = required
                        || "true".equalsIgnoreCase(
                        getAttribute(listbox, "aria-required")
                );

                Locator optionLocator = listbox.locator("[role='option']");

                for (int i = 0; i < optionLocator.count(); i++) {

                    Locator option = optionLocator.nth(i);

                    String optionLabel = option.innerText().trim();

                    String optionValue = getAttribute(option, "data-value");

                    if (optionValue.isBlank()) {
                        optionValue = getAttribute(option, "id");
                    }

                    boolean selected = "true".equalsIgnoreCase(
                            getAttribute(option, "aria-selected")
                    );

                    if (selected) {
                        selectedLabel = optionLabel;
                        selectedValue = optionValue;
                    }

                    options.add(new WorkdayFormOptionInspection(
                            optionLabel,
                            optionValue,
                            selected
                    ));
                }
            }

        } catch (Exception exception) {

            log.warn(
                    "Could not inspect options for Workday dropdown '{}': {}",
                    automationId,
                    exception.getMessage()
            );

        } finally {

            try {
                page.keyboard().press("Escape");
            } catch (Exception exception) {
                log.debug(
                        "Could not close Workday dropdown '{}': {}",
                        automationId,
                        exception.getMessage()
                );
            }
        }

        required = required
                || isRequired(field)
                || isRequired(dropdown);

        boolean hasSelection = !selectedValue.isBlank()
                && !selectedValue.equalsIgnoreCase("select-one")
                && !selectedLabel.equalsIgnoreCase("select one");

        if (hasSelection && !selectedLabel.isBlank()) {
            selectedValues.add(selectedLabel);
        }

        return new WorkdayFormFieldInspection(
                automationId,
                getAttribute(dropdown, "id"),
                getAttribute(dropdown, "name"),
                label,
                "BUTTON",
                "SINGLE_SELECT",
                selectedValue,
                required,
                hasSelection,
                selectedValues,
                options
        );
    }


    private WorkdayFormFieldInspection inspectCheckbox(
            Locator checkbox,
            String automationId,
            String label,
            boolean required
    ) {

        boolean checked = checkbox.isChecked();

        return new WorkdayFormFieldInspection(
                automationId,
                getAttribute(checkbox, "id"),
                getAttribute(checkbox, "name"),
                label,
                "INPUT",
                "CHECKBOX",
                String.valueOf(checked),
                required,
                checked,
                List.of(),
                List.of()
        );
    }

    private WorkdayFormFieldInspection inspectFileUpload(
            Locator fileInput,
            String automationId,
            String label,
            boolean required
    ) {

        String value = "";

        try {
            value = fileInput.inputValue();
        } catch (Exception ignored) {
            // File input value may not be available.
        }

        return new WorkdayFormFieldInspection(
                automationId,
                getAttribute(fileInput, "id"),
                getAttribute(fileInput, "name"),
                label,
                "INPUT",
                "FILE_UPLOAD",
                value,
                required,
                !value.isBlank(),
                List.of(),
                List.of()
        );
    }

    private WorkdayFormFieldInspection inspectInput(
            Locator input,
            String automationId,
            String label,
            boolean required
    ) {

        String tagName = input.evaluate(
                "element => element.tagName"
        ).toString();

        String type = getAttribute(input, "type");

        if (tagName.equalsIgnoreCase("SELECT")) {
            return inspectNativeSelect(
                    input, automationId, label, required
            );
        }

        String value = "";

        try {
            value = input.inputValue();
        } catch (Exception ignored) {
            // Some controls may not expose an input value.
        }

        if (tagName.equalsIgnoreCase("TEXTAREA")) {
            type = "TEXTAREA";
        } else if (type.isBlank()) {
            type = "TEXT";
        }

        return new WorkdayFormFieldInspection(
                automationId,
                getAttribute(input, "id"),
                getAttribute(input, "name"),
                label,
                tagName.toUpperCase(),
                type.toUpperCase(),
                value,
                required,
                false,
                List.of(),
                List.of()
        );
    }

    private WorkdayFormFieldInspection inspectNativeSelect(
            Locator select,
            String automationId,
            String label,
            boolean required
    ) {

        List<WorkdayFormOptionInspection> options =
                new ArrayList<>();

        List<String> selectedValues = new ArrayList<>();

        Locator optionLocator = select.locator("option");

        for (int i = 0; i < optionLocator.count(); i++) {

            Locator option = optionLocator.nth(i);

            String optionLabel = option.innerText().trim();
            String value = getAttribute(option, "value");
            String selectedValue = select.inputValue();
            boolean selected = value.equals(selectedValue);

            if (selected && !optionLabel.isBlank()) {
                selectedValues.add(optionLabel);
            }

            options.add(new WorkdayFormOptionInspection(
                    optionLabel,
                    value,
                    selected
            ));
        }

        String value = "";

        try {
            value = select.inputValue();
        } catch (Exception ignored) {
            // Keep the value empty if it cannot be read.
        }

        return new WorkdayFormFieldInspection(
                automationId,
                getAttribute(select, "id"),
                getAttribute(select, "name"),
                label,
                "SELECT",
                "SINGLE_SELECT",
                value,
                required,
                !value.isBlank(),
                selectedValues,
                options
        );
    }

    private String extractLabel(Locator field) {

        Locator labels = field.locator("label");

        if (labels.count() > 0) {

            String label = labels.first().innerText().trim();

            if (!label.isBlank()) {
                return label.replace("*", "").trim();
            }
        }

        Locator legend = field.locator("legend");

        if (legend.count() > 0) {
            return legend.first().innerText().replace("*", "").trim();
        }

        return "";
    }

    private boolean isRequired(Locator field) {

        return field.locator(
                "[required], [aria-required='true']"
        ).count() > 0
                || field.locator("abbr[aria-hidden='true']").count() > 0;
    }

    private List<String> extractVisibleButtons(Locator root) {

        List<String> buttons = new ArrayList<>();

        Locator locator = root.locator(
                "button, [role='button']"
        );

        for (int i = 0; i < locator.count(); i++) {

            Locator button = locator.nth(i);

            if (!button.isVisible()) {
                continue;
            }

            String text = button.innerText().trim();

            if (text.isBlank()) {
                text = getAttribute(button, "aria-label");
            }

            if (!text.isBlank() && !buttons.contains(text)) {
                buttons.add(text);
            }
        }

        return buttons;
    }

    private boolean hasVisibleElement(Locator locator) {

        for (int i = 0; i < locator.count(); i++) {

            if (locator.nth(i).isVisible()) {
                return true;
            }
        }

        return false;
    }

    private String getAttribute(
            Locator locator,
            String attribute
    ) {

        String value = locator.getAttribute(attribute);

        return value == null ? "" : value.trim();
    }

}
