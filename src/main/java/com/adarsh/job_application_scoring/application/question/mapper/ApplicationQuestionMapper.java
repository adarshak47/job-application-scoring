package com.adarsh.job_application_scoring.application.question.mapper;

import com.adarsh.job_application_scoring.application.question.dto.ApplicationQuestionResponse;
import com.adarsh.job_application_scoring.application.question.model.ApplicationQuestion;
import org.springframework.stereotype.Component;

@Component
public class ApplicationQuestionMapper {

    public ApplicationQuestionResponse toResponse(ApplicationQuestion question) {
        return new ApplicationQuestionResponse(
                question.getId(),
                question.getQuestionKey(),
                question.getCanonicalQuestion(),
                question.getCategory(),
                question.getAnswerType(),
                question.getAnswerValue(),
                question.getAliases()
                        .stream()
                        .map(alias -> alias.getQuestionText())
                        .toList(),
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }
}