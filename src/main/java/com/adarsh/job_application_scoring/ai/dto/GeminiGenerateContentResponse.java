package com.adarsh.job_application_scoring.ai.dto;

import java.util.List;

public record GeminiGenerateContentResponse(
        List<Candidate> candidates
) {
    public record Candidate(
            Content content,
            String finishReason
    ) {
    }

    public record Content(List<Part> parts) {
    }

    public record Part(String text) {
    }
}
