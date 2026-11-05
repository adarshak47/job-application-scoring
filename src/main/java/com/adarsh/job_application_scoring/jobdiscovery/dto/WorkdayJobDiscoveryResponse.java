package com.adarsh.job_application_scoring.jobdiscovery.dto;

import java.util.List;

public record WorkdayJobDiscoveryResponse(
        String sourceUrl,
        int totalJobs,
        int fetchedJobs,
        int pagesFetched,
        List<WorkdayJob> jobs
) {
}
