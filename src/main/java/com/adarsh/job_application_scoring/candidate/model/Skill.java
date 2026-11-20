
package com.adarsh.job_application_scoring.candidate.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "candidate_skill",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_candidate_skill",
                columnNames = {"candidate_id", "name"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private CandidateProfile candidate;

    @Column(nullable = false, length = 150)
    private String name;
}
