package com.adarsh.job_application_scoring.ai.dto;

import java.util.List;
import java.util.Map;

public record GeminiGenerateContentRequest (
        List<Content> contents,
        GenerationConfig generationConfig
) {

    public record Content(List<Part> parts) {

    }

    public record Part(String text) {

    }

    public record GenerationConfig(
            String responseMimeType,
            Map<String, Object> responseSchema,
            double temperature
    ) {

    }
}
