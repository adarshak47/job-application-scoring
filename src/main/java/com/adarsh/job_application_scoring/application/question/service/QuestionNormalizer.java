package com.adarsh.job_application_scoring.application.question.service;

import java.util.Locale;

public final class QuestionNormalizer {

    private QuestionNormalizer() {
    }

    public static String normalize(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question cannot be empty");
        }

        return question
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}