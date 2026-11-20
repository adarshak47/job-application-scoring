
package com.adarsh.job_application_scoring.application.question.controller;

import com.adarsh.job_application_scoring.application.question.dto.CreateQuestionFieldAliasesRequest;
import com.adarsh.job_application_scoring.application.question.dto.QuestionFieldAliasResponse;
import com.adarsh.job_application_scoring.application.question.service.ApplicationQuestionFieldAliasService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/application-question-field-aliases")
@RequiredArgsConstructor
public class ApplicationQuestionFieldAliasController {

    private final ApplicationQuestionFieldAliasService fieldAliasService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<QuestionFieldAliasResponse> createFieldAliases(
            @Valid @RequestBody CreateQuestionFieldAliasesRequest request
    ) {
        return fieldAliasService.createFieldAliases(request);
    }
}
