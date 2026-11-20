package com.adarsh.job_application_scoring.candidate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CandidateProfileRequest(

        @NotNull @Valid
        PersonalInformation personalInformation,

        List<@Valid WorkExperience> workExperiences,

        List<@Valid Education> educations,

        List<@Valid Certification> certifications,

        List<@Valid Language> languages,

        List<@Valid Skill> skills,

        List<@Valid Website> websites,

        List<@Valid SocialNetwork> socialNetworks
) {

    public record PersonalInformation(
            @NotNull @Valid LegalName legalName,
            @Valid PreferredName preferredName,
            @NotBlank String country,
            String gender,
            @Valid Address address,
            @NotBlank @Email String email,
            @Valid Phone phone
    ) {}

    public record LegalName(
            String prefix,
            @NotBlank String givenName,
            @NotBlank String familyName,
            String localGivenName,
            String localFamilyName
    ) {}

    public record PreferredName(
            boolean hasPreferredName,
            String prefix,
            String givenName,
            String familyName,
            String localGivenName,
            String localFamilyName
    ) {}

    public record Address(
            String line1,
            String line2,
            String line3,
            String city,
            String state,
            String postalCode,
            String country
    ) {}

    public record Phone(
            String deviceType,
            String countryCode,
            String number,
            String extension
    ) {}

    public record WorkExperience(
            String jobTitle,
            String company,
            String location,
            boolean currentlyWorkingHere,
            LocalDate startDate,
            LocalDate endDate,
            String roleDescription
    ) {}

    public record Education(
            String schoolOrUniversity,
            String degree,
            String fieldOfStudy,
            String cgpa,
            LocalDate startDate,
            LocalDate endDate
    ) {}

    public record Certification(
            String name,
            LocalDate issueDate,
            LocalDate expiryDate
    ) {}

    public record Language(
            @NotBlank String language,
            boolean fluent,
            String proficiency
    ) {}

    public record Skill(
            @NotBlank String name
    ) {}

    public record Website(
            String name,
            String url
    ) {}

    public record SocialNetwork(
            String platform,
            String url
    ) {}
}