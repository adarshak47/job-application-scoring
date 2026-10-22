package com.adarsh.job_application_scoring.evaluation.controller;

import com.adarsh.job_application_scoring.evaluation.dto.request.EvaluateJobRequest;
import com.adarsh.job_application_scoring.evaluation.dto.response.JobEvaluationResponse;
import com.adarsh.job_application_scoring.evaluation.service.JobEvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/evaluations")
@RequiredArgsConstructor
public class JobEvaluationController {

    private final JobEvaluationService jobEvaluationService;

    @PostMapping
    public ResponseEntity<JobEvaluationResponse> evaluate(@Valid @RequestBody EvaluateJobRequest evaluateJobRequest) {
        JobEvaluationResponse jobEvaluationResponse = jobEvaluationService.evaluate(evaluateJobRequest);
        return ResponseEntity.ok(jobEvaluationResponse);
    }

}
