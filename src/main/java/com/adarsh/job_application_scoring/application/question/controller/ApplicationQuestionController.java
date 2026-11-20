package com.adarsh.job_application_scoring.application.question.controller;

import com.adarsh.job_application_scoring.application.question.dto.*;
import com.adarsh.job_application_scoring.application.question.service.ApplicationQuestionResolutionService;
import com.adarsh.job_application_scoring.application.question.service.ApplicationQuestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/application-questions")
public class ApplicationQuestionController {

    private final ApplicationQuestionService service;
    private final ApplicationQuestionResolutionService resolutionService;

    public ApplicationQuestionController(
            ApplicationQuestionService service,
            ApplicationQuestionResolutionService resolutionService
    ) {
        this.service = service;
        this.resolutionService = resolutionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationQuestionResponse create(
            @Valid @RequestBody ApplicationQuestionRequest request
    ) {
        return service.createQuestion(request);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ApplicationQuestionResponse> createBulk(
            @Valid @RequestBody BulkApplicationQuestionRequest request
    ) {
        return service.createQuestions(request);
    }

    @PostMapping("/{questionId}/aliases")
    public ApplicationQuestionResponse addAlias(
            @PathVariable Long questionId,
            @RequestBody String aliasText
    ) {
        return service.addAlias(questionId, aliasText);
    }

    @PostMapping("/{questionId}/aliases/bulk")
    public ApplicationQuestionResponse addAliases(
            @PathVariable Long questionId,
            @Valid @RequestBody BulkAliasRequest request
    ) {
        return service.addAliases(questionId, request);
    }

    @PutMapping("/{questionId}")
    public ApplicationQuestionResponse updateQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody UpdateApplicationQuestionRequest request
    ) {
        return service.updateQuestion(questionId, request);
    }

    @PutMapping("/{questionId}/answer")
    public ApplicationQuestionResponse updateAnswer(
            @PathVariable Long questionId,
            @Valid @RequestBody UpdateApplicationAnswerRequest request
    ) {
        return service.updateAnswer(questionId, request);
    }

    @GetMapping("/{questionKey}")
    public ApplicationQuestionResponse getByKey(
            @PathVariable String questionKey
    ) {
        return service.getByQuestionKey(questionKey);
    }

    @GetMapping("/category/{category}")
    public List<ApplicationQuestionResponse> getByCategory(
            @PathVariable String category
    ) {
        return service.getQuestionsByCategory(category);
    }

    @GetMapping
    public List<ApplicationQuestionResponse> getAll() {
        return service.getAllQuestions();
    }

    @PostMapping("/resolve")
    public ApplicationQuestionResolutionResponse resolve(
            @Valid @RequestBody ApplicationQuestionResolutionRequest request
    ) {
        return resolutionService.resolve(request);
    }

    @DeleteMapping("/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long questionId) {
        service.deleteQuestion(questionId);
    }
}