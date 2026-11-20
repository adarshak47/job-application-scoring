
package com.adarsh.job_application_scoring.candidate.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "candidate_profile")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private PersonalInformation personalInformation;

    @Builder.Default
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkExperience> workExperiences = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Education> educations = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Certification> certifications = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Language> languages = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Skill> skills = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Website> websites = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SocialNetwork> socialNetworks = new ArrayList<>();

    @OneToOne(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private ResumeDocument resume;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addWorkExperience(WorkExperience experience) {
        workExperiences.add(experience);
        experience.setCandidate(this);
    }

    public void addEducation(Education education) {
        educations.add(education);
        education.setCandidate(this);
    }

    public void addCertification(Certification certification) {
        certifications.add(certification);
        certification.setCandidate(this);
    }

    public void addSkill(Skill skill) {
        skills.add(skill);
        skill.setCandidate(this);
    }

    public void addLanguage(Language language) {
        languages.add(language);
        language.setCandidate(this);
    }

    public void addWebsite(Website website) {
        websites.add(website);
        website.setCandidate(this);
    }

    public void addSocialNetwork(SocialNetwork socialNetwork) {
        socialNetworks.add(socialNetwork);
        socialNetwork.setCandidate(this);
    }

    public void clearWorkExperiences() {
        workExperiences.clear();
    }

    public void clearEducations() {
        educations.clear();
    }

    public void clearCertifications() {
        certifications.clear();
    }

    public void clearLanguages() {
        languages.clear();
    }

    public void clearSkills() {
        skills.clear();
    }

    public void clearWebsites() {
        websites.clear();
    }

    public void clearSocialNetworks() {
        socialNetworks.clear();
    }

    public void setResumeDocument(ResumeDocument resume) {
        this.resume = resume;

        if (resume != null) {
            resume.setCandidate(this);
        }
    }
}
