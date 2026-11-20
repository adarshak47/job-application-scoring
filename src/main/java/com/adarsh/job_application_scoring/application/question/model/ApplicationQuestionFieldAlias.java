
package com.adarsh.job_application_scoring.application.question.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "application_question_field_alias",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_application_question_field_alias_identifier",
                        columnNames = {"identifier_type", "field_identifier"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_application_question_field_alias_question_id",
                        columnList = "question_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ApplicationQuestionFieldAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private ApplicationQuestion question;

    @Enumerated(EnumType.STRING)
    @Column(name = "identifier_type", nullable = false, length = 30)
    private ApplicationQuestionFieldIdentifierType identifierType;

    @Column(name = "field_identifier", nullable = false, length = 255)
    private String fieldIdentifier;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
