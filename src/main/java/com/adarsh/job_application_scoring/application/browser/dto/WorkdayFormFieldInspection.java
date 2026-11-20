package com.adarsh.job_application_scoring.application.browser.dto;

import java.util.List;

public record WorkdayFormFieldInspection(
        String automationId,
        String id,
        String name,
        String label,
        String tagName,
        String type,
        String value,
        boolean required,
        boolean checked,
        List<String> selectedValues,
        List<WorkdayFormOptionInspection> options
) {
}