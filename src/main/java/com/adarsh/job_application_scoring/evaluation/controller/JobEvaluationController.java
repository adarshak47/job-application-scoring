package com.adarsh.job_application_scoring.evaluation.controller;

import com.adarsh.job_application_scoring.evaluation.dto.request.EvaluateJobRequest;
import com.adarsh.job_application_scoring.evaluation.dto.response.JobEvaluationResponse;
import com.adarsh.job_application_scoring.evaluation.service.JobEvaluationService;
import com.adarsh.job_application_scoring.resume.service.PdfTextExtractor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/evaluations")
@RequiredArgsConstructor
public class JobEvaluationController {

    private final JobEvaluationService jobEvaluationService;

    private final PdfTextExtractor pdfTextExtractor;

    @PostMapping
    public ResponseEntity<JobEvaluationResponse> evaluate(@Valid @RequestBody EvaluateJobRequest evaluateJobRequest) {
        JobEvaluationResponse jobEvaluationResponse = jobEvaluationService.evaluate(evaluateJobRequest);
        return ResponseEntity.ok(jobEvaluationResponse);
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<JobEvaluationResponse> evaluate(
            @RequestPart("resume") MultipartFile resume,
            @RequestParam("jobDescription") String jobDescription
    ) {
        String extractedResumeText = pdfTextExtractor.extractText(resume);

        EvaluateJobRequest request = new EvaluateJobRequest(extractedResumeText, jobDescription);
        JobEvaluationResponse response = jobEvaluationService.evaluate(request);

        return ResponseEntity.ok(response);
    }

}
