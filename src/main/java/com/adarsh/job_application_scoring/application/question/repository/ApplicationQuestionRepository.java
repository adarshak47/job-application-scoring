package com.adarsh.job_application_scoring.application.question.repository;

import com.adarsh.job_application_scoring.application.question.model.ApplicationQuestion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApplicationQuestionRepository
        extends JpaRepository<ApplicationQuestion, Long> {

    boolean existsByQuestionKey(String questionKey);

    @EntityGraph(attributePaths = "aliases")
    Optional<ApplicationQuestion> findByQuestionKey(String questionKey);

    @EntityGraph(attributePaths = "aliases")
    @Query("""
            SELECT q
            FROM ApplicationQuestion q
            JOIN q.aliases a
            WHERE a.normalizedText = :normalizedText
            """)
    Optional<ApplicationQuestion> findByAliasNormalizedText(
            @Param("normalizedText") String normalizedText
    );

    @EntityGraph(attributePaths = "aliases")
    @Query("""
            SELECT q
            FROM ApplicationQuestion q
            ORDER BY q.id
            """)
    List<ApplicationQuestion> findAllWithAliases();

    @EntityGraph(attributePaths = "aliases")
    @Query("""
            SELECT q
            FROM ApplicationQuestion q
            WHERE LOWER(q.category) = LOWER(:category)
            ORDER BY q.id
            """)
    List<ApplicationQuestion> findAllByCategory(
            @Param("category") String category
    );
}