package com.adarsh.job_application_scoring.candidate.service;

import com.adarsh.job_application_scoring.candidate.dto.CandidateProfileRequest;
import com.adarsh.job_application_scoring.candidate.dto.CandidateProfileResponse;
import com.adarsh.job_application_scoring.candidate.mapper.CandidateProfileMapper;
import com.adarsh.job_application_scoring.candidate.model.CandidateProfile;
import com.adarsh.job_application_scoring.candidate.repository.CandidateProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CandidateProfileService {

    private final CandidateProfileRepository candidateProfileRepository;

    private final CandidateProfileMapper candidateProfileMapper;

    @Transactional
    public CandidateProfileResponse create(CandidateProfileRequest candidateProfileRequest) {
        if(candidateProfileRepository.findFirstByOrderByIdAsc().isPresent()) {
            throw new IllegalStateException(
                    "Candidate profile already exists. Use the update operation."
            );
        }

        CandidateProfile candidateProfile = candidateProfileMapper.toEntity(candidateProfileRequest);
        CandidateProfile savedProfile = candidateProfileRepository.save(candidateProfile);
        return candidateProfileMapper.toResponse(savedProfile);
    }

    @Transactional(readOnly = true)
    public CandidateProfileResponse get() {

        CandidateProfile candidateProfile = candidateProfileRepository
                .findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException(
                        "Candidate profile has not been created yet."
                ));

        return candidateProfileMapper.toResponse(candidateProfile);
    }

    @Transactional
    public CandidateProfileResponse update(CandidateProfileRequest request) {
        CandidateProfile profile = candidateProfileRepository
                .findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException(
                        "Candidate profile has not been created yet."
                ));

        candidateProfileMapper.updateEntity(request, profile);

        CandidateProfile updatedProfile = candidateProfileRepository.save(profile);

        return candidateProfileMapper.toResponse(updatedProfile);
    }
}
