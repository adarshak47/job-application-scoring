package com.adarsh.job_application_scoring.application.data.dto;

import java.util.List;

public record CandidateDataReadiness(
        List<String> availableFields,
        List<String> missingFields,
        List<String> emptyCollections,
        boolean resumeAvailable
) {
}