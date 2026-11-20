package com.adarsh.job_application_scoring.candidate.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CandidateProfileResponse(
        Long id,
        CandidateProfileRequest.PersonalInformation personalInformation,
        List<CandidateProfileRequest.WorkExperience> workExperiences,
        List<CandidateProfileRequest.Education> educations,
        List<CandidateProfileRequest.Certification> certifications,
        List<CandidateProfileRequest.Language> languages,
        List<CandidateProfileRequest.Skill> skills,
        List<CandidateProfileRequest.Website> websites,
        List<CandidateProfileRequest.SocialNetwork> socialNetworks,
        ResumeDocumentResponse resume,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record ResumeDocumentResponse(
            Long id,
            String fileName
    ) {}
}
