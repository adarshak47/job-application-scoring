
package com.adarsh.job_application_scoring.candidate.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "education")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Education {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private CandidateProfile candidate;

    @Column(length = 255)
    private String schoolOrUniversity;

    @Column(length = 150)
    private String degree;

    @Column(length = 150)
    private String fieldOfStudy;

    @Column(length = 20)
    private String cgpa;

    private LocalDate startDate;

    private LocalDate endDate;
}
