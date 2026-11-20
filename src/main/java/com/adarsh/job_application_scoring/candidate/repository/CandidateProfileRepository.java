package com.adarsh.job_application_scoring.candidate.repository;

import com.adarsh.job_application_scoring.candidate.model.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long> {

    Optional<CandidateProfile> findFirstByOrderByIdAsc();
}