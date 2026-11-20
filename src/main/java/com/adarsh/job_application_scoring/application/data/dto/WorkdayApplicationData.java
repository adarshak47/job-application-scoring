package com.adarsh.job_application_scoring.application.data.dto;

public record WorkdayApplicationData(
        WorkdayPersonalInformation personalInformation,
        WorkdayCandidateCollections candidateCollections,
        WorkdayResumeData resume,
        CandidateDataReadiness readiness
) {
}