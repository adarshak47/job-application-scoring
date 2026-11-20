
package com.adarsh.job_application_scoring.application.question.service;

import com.adarsh.job_application_scoring.application.question.dto.ApplicationQuestionFieldIdentifier;
import com.adarsh.job_application_scoring.application.question.dto.ApplicationQuestionFieldResolutionResult;
import com.adarsh.job_application_scoring.application.question.model.ApplicationQuestion;
import com.adarsh.job_application_scoring.application.question.repository.ApplicationQuestionFieldAliasRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ApplicationQuestionFieldResolutionService {

    private final ApplicationQuestionFieldAliasRepository fieldAliasRepository;

    @Transactional(readOnly = true)
    public ApplicationQuestionFieldResolutionResult resolve(
            List<ApplicationQuestionFieldIdentifier> identifiers
    ) {

        if (identifiers == null || identifiers.isEmpty()) {
            return unresolved(
                    "NO_IDENTIFIERS",
                    "No field identifiers were provided."
            );
        }

        ApplicationQuestion matchedQuestion = null;

        for (ApplicationQuestionFieldIdentifier identifier : identifiers) {

            if (identifier == null
                    || identifier.identifierType() == null
                    || identifier.fieldIdentifier() == null
                    || identifier.fieldIdentifier().isBlank()) {
                continue;
            }

            String fieldIdentifier = identifier.fieldIdentifier().trim();

            var fieldAlias = fieldAliasRepository
                    .findByIdentifierTypeAndFieldIdentifier(
                            identifier.identifierType(),
                            fieldIdentifier
                    );

            if (fieldAlias.isEmpty()) {
                continue;
            }

            ApplicationQuestion currentQuestion =
                    fieldAlias.get().getQuestion();

            if (matchedQuestion == null) {
                matchedQuestion = currentQuestion;
                continue;
            }

            if (!Objects.equals(
                    matchedQuestion.getQuestionKey(),
                    currentQuestion.getQuestionKey()
            )) {
                return unresolved(
                        "CONFLICTING_FIELD_MAPPINGS",
                        "The supplied field identifiers map to different canonical questions."
                );
            }
        }

        if (matchedQuestion == null) {
            return unresolved(
                    "FIELD_NOT_REGISTERED",
                    "None of the supplied field identifiers is registered."
            );
        }

        if (matchedQuestion.getAnswerType() == null) {
            return unresolved(
                    "ANSWER_TYPE_MISSING",
                    "The canonical question has no configured answer type."
            );
        }

        if (matchedQuestion.getAnswerValue() == null
                || matchedQuestion.getAnswerValue().isNull()) {
            return unresolved(
                    "ANSWER_NOT_CONFIGURED",
                    "No answer has been configured for the canonical question."
            );
        }

        return new ApplicationQuestionFieldResolutionResult(
                true,
                matchedQuestion.getQuestionKey(),
                matchedQuestion.getCanonicalQuestion(),
                matchedQuestion.getAnswerType().name(),
                matchedQuestion.getAnswerValue(),
                "RESOLVED",
                "Field identifiers successfully resolved."
        );
    }

    private ApplicationQuestionFieldResolutionResult unresolved(
            String status,
            String message
    ) {
        return new ApplicationQuestionFieldResolutionResult(
                false,
                null,
                null,
                null,
                null,
                status,
                message
        );
    }
}
