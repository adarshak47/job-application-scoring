
package com.adarsh.job_application_scoring.application.question.repository;

import com.adarsh.job_application_scoring.application.question.model.ApplicationQuestionFieldAlias;
import com.adarsh.job_application_scoring.application.question.model.ApplicationQuestionFieldIdentifierType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicationQuestionFieldAliasRepository
        extends JpaRepository<ApplicationQuestionFieldAlias, Long> {

    @EntityGraph(attributePaths = "question")
    Optional<ApplicationQuestionFieldAlias> findByIdentifierTypeAndFieldIdentifier(
            ApplicationQuestionFieldIdentifierType identifierType,
            String fieldIdentifier
    );

    boolean existsByIdentifierTypeAndFieldIdentifier(
            ApplicationQuestionFieldIdentifierType identifierType,
            String fieldIdentifier
    );
}
