package com.adarsh.job_application_scoring.jobdiscovery.controller;

import com.adarsh.job_application_scoring.jobdiscovery.dto.WorkdayJobDiscoveryRequest;
import com.adarsh.job_application_scoring.jobdiscovery.dto.WorkdayJobDiscoveryResponse;
import com.adarsh.job_application_scoring.jobdiscovery.service.WorkdayJobDiscoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/job-discovery/workday")
@RequiredArgsConstructor
public class WorkdayJobDiscoveryController {

    private final WorkdayJobDiscoveryService workdayJobDiscoveryService;

    @PostMapping
    public ResponseEntity<WorkdayJobDiscoveryResponse> discoverJobs(
            @Valid @RequestBody WorkdayJobDiscoveryRequest request
    ) {
        return ResponseEntity.ok(workdayJobDiscoveryService.discoverJobs(request));
    }
}
