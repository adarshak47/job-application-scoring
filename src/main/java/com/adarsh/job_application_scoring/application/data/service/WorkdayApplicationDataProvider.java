package com.adarsh.job_application_scoring.application.data.service;

import com.adarsh.job_application_scoring.application.data.dto.CandidateDataReadiness;
import com.adarsh.job_application_scoring.application.data.dto.WorkdayApplicationData;
import com.adarsh.job_application_scoring.application.data.dto.WorkdayCandidateCollections;
import com.adarsh.job_application_scoring.application.data.dto.WorkdayPersonalInformation;
import com.adarsh.job_application_scoring.application.data.dto.WorkdayResumeData;
import com.adarsh.job_application_scoring.application.data.mapper.WorkdayCandidateCollectionsMapper;
import com.adarsh.job_application_scoring.application.data.mapper.WorkdayPersonalInformationMapper;
import com.adarsh.job_application_scoring.application.data.mapper.WorkdayResumeMapper;
import com.adarsh.job_application_scoring.application.data.validator.CandidateDataReadinessValidator;
import com.adarsh.job_application_scoring.candidate.model.CandidateProfile;
import com.adarsh.job_application_scoring.candidate.repository.CandidateProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkdayApplicationDataProvider {

    private final CandidateProfileRepository candidateProfileRepository;

    private final WorkdayPersonalInformationMapper personalInformationMapper;
    private final WorkdayCandidateCollectionsMapper collectionsMapper;
    private final WorkdayResumeMapper resumeMapper;
    private final CandidateDataReadinessValidator readinessValidator;

    @Transactional(readOnly = true)
    public WorkdayApplicationData getApplicationData() {

        CandidateProfile candidateProfile = candidateProfileRepository
                .findFirstByOrderByIdAsc()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Candidate profile not found. Please create a candidate profile first."
                ));

        WorkdayPersonalInformation personalInformation =
                personalInformationMapper.toDto(candidateProfile);

        WorkdayCandidateCollections candidateCollections =
                collectionsMapper.toDto(candidateProfile);

        WorkdayResumeData resume =
                resumeMapper.toDto(candidateProfile);

        CandidateDataReadiness readiness = readinessValidator.validate(
                personalInformation,
                candidateCollections,
                resume
        );

        return new WorkdayApplicationData(
                personalInformation,
                candidateCollections,
                resume,
                readiness
        );
    }
}