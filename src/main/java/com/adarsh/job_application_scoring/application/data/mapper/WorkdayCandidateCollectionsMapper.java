package com.adarsh.job_application_scoring.application.data.mapper;

import com.adarsh.job_application_scoring.application.data.dto.WorkdayCandidateCollections;
import com.adarsh.job_application_scoring.candidate.model.CandidateProfile;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class WorkdayCandidateCollectionsMapper {

    public WorkdayCandidateCollections toDto(CandidateProfile candidateProfile) {

        return new WorkdayCandidateCollections(
                mapWorkExperiences(candidateProfile),
                mapEducations(candidateProfile),
                mapCertifications(candidateProfile),
                mapLanguages(candidateProfile),
                mapSkills(candidateProfile),
                mapWebsites(candidateProfile),
                mapSocialNetworks(candidateProfile)
        );
    }

    private List<WorkdayCandidateCollections.WorkExperienceData> mapWorkExperiences(
            CandidateProfile profile) {

        if (profile.getWorkExperiences() == null) {
            return Collections.emptyList();
        }

        return profile.getWorkExperiences().stream()
                .map(experience -> new WorkdayCandidateCollections.WorkExperienceData(
                        experience.getJobTitle(),
                        experience.getCompany(),
                        experience.getLocation(),
                        experience.isCurrentlyWorkingHere(),
                        experience.getStartDate(),
                        experience.getEndDate(),
                        experience.getRoleDescription()
                ))
                .toList();
    }

    private List<WorkdayCandidateCollections.EducationData> mapEducations(
            CandidateProfile profile) {

        if (profile.getEducations() == null) {
            return Collections.emptyList();
        }

        return profile.getEducations().stream()
                .map(education -> new WorkdayCandidateCollections.EducationData(
                        education.getSchoolOrUniversity(),
                        education.getDegree(),
                        education.getFieldOfStudy(),
                        education.getCgpa(),
                        education.getStartDate(),
                        education.getEndDate()
                ))
                .toList();
    }

    private List<WorkdayCandidateCollections.CertificationData> mapCertifications(
            CandidateProfile profile) {

        if (profile.getCertifications() == null) {
            return Collections.emptyList();
        }

        return profile.getCertifications().stream()
                .map(certification -> new WorkdayCandidateCollections.CertificationData(
                        certification.getName(),
                        certification.getIssueDate(),
                        certification.getExpiryDate(),
                        certification.getFilePath()
                ))
                .toList();
    }

    private List<WorkdayCandidateCollections.LanguageData> mapLanguages(
            CandidateProfile profile) {

        if (profile.getLanguages() == null) {
            return Collections.emptyList();
        }

        return profile.getLanguages().stream()
                .map(language -> new WorkdayCandidateCollections.LanguageData(
                        language.getLanguage(),
                        language.isFluent(),
                        language.getProficiency()
                ))
                .toList();
    }

    private List<WorkdayCandidateCollections.SkillData> mapSkills(
            CandidateProfile profile) {

        if (profile.getSkills() == null) {
            return Collections.emptyList();
        }

        return profile.getSkills().stream()
                .map(skill -> new WorkdayCandidateCollections.SkillData(
                        skill.getName()
                ))
                .toList();
    }

    private List<WorkdayCandidateCollections.WebsiteData> mapWebsites(
            CandidateProfile profile) {

        if (profile.getWebsites() == null) {
            return Collections.emptyList();
        }

        return profile.getWebsites().stream()
                .map(website -> new WorkdayCandidateCollections.WebsiteData(
                        website.getName(),
                        website.getUrl()
                ))
                .toList();
    }

    private List<WorkdayCandidateCollections.SocialNetworkData> mapSocialNetworks(
            CandidateProfile profile) {

        if (profile.getSocialNetworks() == null) {
            return Collections.emptyList();
        }

        return profile.getSocialNetworks().stream()
                .map(socialNetwork -> new WorkdayCandidateCollections.SocialNetworkData(
                        socialNetwork.getPlatform(),
                        socialNetwork.getUrl()
                ))
                .toList();
    }
}