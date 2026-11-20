package com.adarsh.job_application_scoring.application.question.service;

import com.adarsh.job_application_scoring.application.question.dto.ApplicationQuestionResolutionRequest;
import com.adarsh.job_application_scoring.application.question.dto.ApplicationQuestionResolutionResponse;
import com.adarsh.job_application_scoring.application.question.dto.InvalidApplicationQuestion;
import com.adarsh.job_application_scoring.application.question.dto.ResolvedApplicationQuestion;
import com.adarsh.job_application_scoring.application.question.model.ApplicationQuestion;
import com.adarsh.job_application_scoring.application.question.repository.ApplicationQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ApplicationQuestionResolutionService {

    private final ApplicationQuestionRepository repository;
    private final AnswerValueValidator answerValueValidator;

    public ApplicationQuestionResolutionService(
            ApplicationQuestionRepository repository,
            AnswerValueValidator answerValueValidator
    ) {
        this.repository = repository;
        this.answerValueValidator = answerValueValidator;
    }

    @Transactional(readOnly = true)
    public ApplicationQuestionResolutionResponse resolve(
            ApplicationQuestionResolutionRequest request
    ) {
        List<ResolvedApplicationQuestion> resolvedQuestions =
                new ArrayList<>();

        List<String> unknownQuestions = new ArrayList<>();

        List<InvalidApplicationQuestion> invalidQuestions =
                new ArrayList<>();

        for (String originalQuestion : request.questions()) {

            String normalizedText =
                    QuestionNormalizer.normalize(originalQuestion);

            ApplicationQuestion question = repository
                    .findByAliasNormalizedText(normalizedText)
                    .orElse(null);

            if (question == null) {
                unknownQuestions.add(originalQuestion);
                continue;
            }

            try {
                answerValueValidator.validate(
                        question.getAnswerType(),
                        question.getAnswerValue()
                );

                resolvedQuestions.add(
                        new ResolvedApplicationQuestion(
                                originalQuestion,
                                question.getQuestionKey(),
                                question.getAnswerType(),
                                question.getAnswerValue()
                        )
                );

            } catch (IllegalArgumentException exception) {

                invalidQuestions.add(
                        new InvalidApplicationQuestion(
                                originalQuestion,
                                question.getQuestionKey(),
                                exception.getMessage()
                        )
                );
            }
        }

        boolean canProceed =
                unknownQuestions.isEmpty()
                        && invalidQuestions.isEmpty();

        return new ApplicationQuestionResolutionResponse(
                canProceed,
                resolvedQuestions,
                unknownQuestions,
                invalidQuestions
        );
    }
}