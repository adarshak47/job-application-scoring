package com.adarsh.job_application_scoring.application.data.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record WorkdayCandidateCollections(
        List<WorkExperienceData> workExperiences,
        List<EducationData> educations,
        List<CertificationData> certifications,
        List<LanguageData> languages,
        List<SkillData> skills,
        List<WebsiteData> websites,
        List<SocialNetworkData> socialNetworks
) {

    public record WorkExperienceData(
            String jobTitle,
            String company,
            String location,
            Boolean currentlyWorkingHere,
            LocalDate startDate,
            LocalDate endDate,
            String roleDescription
    ) {
    }

    public record EducationData(
            String schoolOrUniversity,
            String degree,
            String fieldOfStudy,
            String cgpa,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    public record CertificationData(
            String name,
            LocalDate issueDate,
            LocalDate expiryDate,
            String filePath
    ) {
    }

    public record LanguageData(
            String language,
            Boolean fluent,
            String proficiency
    ) {
    }

    public record SkillData(
            String name
    ) {
    }

    public record WebsiteData(
            String name,
            String url
    ) {
    }

    public record SocialNetworkData(
            String platform,
            String url
    ) {
    }
}