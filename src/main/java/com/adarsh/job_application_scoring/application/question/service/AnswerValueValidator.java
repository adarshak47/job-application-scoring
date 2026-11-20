package com.adarsh.job_application_scoring.application.question.service;

import com.adarsh.job_application_scoring.application.question.model.AnswerType;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AnswerValueValidator {

    public void validate(AnswerType answerType, JsonNode answerValue) {

        if (answerType == null || answerValue == null || answerValue.isNull()) {
            throw new IllegalArgumentException("Answer type and value are required");
        }

        boolean valid = switch (answerType) {
            case TEXT -> answerValue.isTextual()
                    && !answerValue.asText().isBlank();

            case BOOLEAN -> answerValue.isBoolean();

            case NUMBER -> answerValue.isNumber();

            case DATE -> isValidDate(answerValue);

            case SINGLE_SELECT -> answerValue.isTextual()
                    && !answerValue.asText().isBlank();

            case MULTI_SELECT -> isStringArray(answerValue);
        };

        if (!valid) {
            throw new IllegalArgumentException(
                    "Invalid answer value for answer type: " + answerType
            );
        }
    }

    private boolean isValidDate(JsonNode value) {
        if (!value.isTextual()) {
            return false;
        }

        try {
            LocalDate.parse(value.asText());
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean isStringArray(JsonNode value) {
        if (!value.isArray() || value.isEmpty()) {
            return false;
        }

        for (JsonNode element : value) {
            if (!element.isTextual() || element.asText().isBlank()) {
                return false;
            }
        }

        return true;
    }
}