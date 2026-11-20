package com.adarsh.job_application_scoring.application.browser.controller;

import com.adarsh.job_application_scoring.application.browser.dto.*;
import com.adarsh.job_application_scoring.application.browser.service.WorkdayApplicationBrowserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workday/browser")
@RequiredArgsConstructor
public class WorkdayBrowserController {

    private final WorkdayApplicationBrowserService browserService;


    @PostMapping("/inspect")
    public WorkdayBrowserResponse inspect(
            @Valid @RequestBody WorkdayBrowserRequest request) {

        return browserService.inspectApplicationPage(
                request.applicationUrl()
        );
    }

    @PostMapping("/state")
    public WorkdayApplicationStateResponse inspectState(
            @Valid @RequestBody WorkdayBrowserRequest request) {

        return browserService.inspectApplicationState(
                request.applicationUrl()
        );
    }

    @PostMapping("/start")
    public ResponseEntity<WorkdayApplicationStartResult> startApplication(
            @Valid @RequestBody WorkdayApplyRequest request
    ) {
        return ResponseEntity.ok(
                browserService.startApplication(
                        request.applicationUrl()
                )
        );
    }
}