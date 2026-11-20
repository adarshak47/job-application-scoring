
package com.adarsh.job_application_scoring.candidate.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "candidate_social_network")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialNetwork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private CandidateProfile candidate;

    @Column(length = 100)
    private String platform;

    @Column(length = 1000)
    private String url;
}
