
package com.adarsh.job_application_scoring.application.question.service;

import com.adarsh.job_application_scoring.application.question.dto.CreateQuestionFieldAliasesRequest;
import com.adarsh.job_application_scoring.application.question.dto.QuestionFieldAliasResponse;
import com.adarsh.job_application_scoring.application.question.model.ApplicationQuestion;
import com.adarsh.job_application_scoring.application.question.model.ApplicationQuestionFieldAlias;
import com.adarsh.job_application_scoring.application.question.repository.ApplicationQuestionFieldAliasRepository;
import com.adarsh.job_application_scoring.application.question.repository.ApplicationQuestionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApplicationQuestionFieldAliasService {

    private final ApplicationQuestionRepository applicationQuestionRepository;
    private final ApplicationQuestionFieldAliasRepository fieldAliasRepository;

    @Transactional
    public List<QuestionFieldAliasResponse> createFieldAliases(
            CreateQuestionFieldAliasesRequest request
    ) {

        ApplicationQuestion question = applicationQuestionRepository
                .findByQuestionKey(request.questionKey())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Application question not found: " + request.questionKey()
                ));

        Set<String> requestIdentifiers = new HashSet<>();

        List<ApplicationQuestionFieldAlias> aliasesToSave = new ArrayList<>();

        for (CreateQuestionFieldAliasesRequest.FieldAliasRequest fieldAliasRequest
                : request.fieldAliases()) {

            String identifier = fieldAliasRequest.fieldIdentifier().trim();

            String uniqueKey = fieldAliasRequest.identifierType().name()
                    + "::" + identifier;

            if (!requestIdentifiers.add(uniqueKey)) {
                throw new IllegalArgumentException(
                        "Duplicate field identifier in request: " + identifier
                );
            }

            boolean alreadyExists = fieldAliasRepository
                    .existsByIdentifierTypeAndFieldIdentifier(
                            fieldAliasRequest.identifierType(),
                            identifier
                    );

            if (alreadyExists) {
                throw new IllegalArgumentException(
                        "Field identifier is already registered: "
                                + fieldAliasRequest.identifierType()
                                + " - " + identifier
                );
            }

            ApplicationQuestionFieldAlias alias =
                    new ApplicationQuestionFieldAlias();

            alias.setQuestion(question);
            alias.setIdentifierType(fieldAliasRequest.identifierType());
            alias.setFieldIdentifier(identifier);

            aliasesToSave.add(alias);
        }

        return fieldAliasRepository.saveAll(aliasesToSave)
                .stream()
                .map(alias -> new QuestionFieldAliasResponse(
                        alias.getId(),
                        question.getQuestionKey(),
                        alias.getIdentifierType(),
                        alias.getFieldIdentifier()
                ))
                .toList();
    }
}
