package com.adarsh.job_application_scoring.jobdiscovery.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record WorkdayJobDiscoveryRequest(
        @NotBlank String sourceUrl,
        @Min(1) @Max(100) Integer pageSize,
        String country,
        String location,
        List<String> includeTitles,
        @Min(1) @Max(365) Integer postedWithinDays
) {
}