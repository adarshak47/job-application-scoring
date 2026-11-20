package com.adarsh.job_application_scoring.candidate.controller;

import com.adarsh.job_application_scoring.candidate.dto.CandidateProfileRequest;
import com.adarsh.job_application_scoring.candidate.dto.CandidateProfileResponse;
import com.adarsh.job_application_scoring.candidate.service.CandidateProfileService;
import com.adarsh.job_application_scoring.candidate.service.CandidateResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/candidate-profile")
@RequiredArgsConstructor
public class CandidateProfileController {

    private final CandidateProfileService candidateProfileService;

    private final CandidateResumeService candidateResumeService;

    @PostMapping
    public ResponseEntity<CandidateProfileResponse> create(
            @Valid @RequestBody CandidateProfileRequest request
    ) {
        CandidateProfileResponse response = candidateProfileService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<CandidateProfileResponse> get() {
        return ResponseEntity.ok(candidateProfileService.get());
    }

    @PutMapping
    public ResponseEntity<CandidateProfileResponse> update(
            @Valid @RequestBody CandidateProfileRequest request
    ) {
        return ResponseEntity.ok(candidateProfileService.update(request));
    }

    @PostMapping(
            value = "/resume",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<CandidateProfileResponse> uploadResume(
            @RequestPart("resume") MultipartFile resume
    ) {
        return ResponseEntity.ok(candidateResumeService.upload(resume));
    }
}