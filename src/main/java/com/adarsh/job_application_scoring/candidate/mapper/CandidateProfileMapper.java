package com.adarsh.job_application_scoring.candidate.mapper;

import com.adarsh.job_application_scoring.candidate.dto.CandidateProfileRequest;
import com.adarsh.job_application_scoring.candidate.dto.CandidateProfileResponse;
import com.adarsh.job_application_scoring.candidate.model.Address;
import com.adarsh.job_application_scoring.candidate.model.CandidateProfile;
import com.adarsh.job_application_scoring.candidate.model.Certification;
import com.adarsh.job_application_scoring.candidate.model.Education;
import com.adarsh.job_application_scoring.candidate.model.Language;
import com.adarsh.job_application_scoring.candidate.model.LegalName;
import com.adarsh.job_application_scoring.candidate.model.PersonalInformation;
import com.adarsh.job_application_scoring.candidate.model.Phone;
import com.adarsh.job_application_scoring.candidate.model.PreferredName;
import com.adarsh.job_application_scoring.candidate.model.ResumeDocument;
import com.adarsh.job_application_scoring.candidate.model.Skill;
import com.adarsh.job_application_scoring.candidate.model.SocialNetwork;
import com.adarsh.job_application_scoring.candidate.model.Website;
import com.adarsh.job_application_scoring.candidate.model.WorkExperience;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
public class CandidateProfileMapper {

    public CandidateProfile toEntity(CandidateProfileRequest request) {
        CandidateProfile profile = new CandidateProfile();

        profile.setPersonalInformation(toPersonalInformation(
                request.personalInformation()
        ));

        mapList(request.workExperiences(), this::toWorkExperience)
                .forEach(profile::addWorkExperience);

        mapList(request.educations(), this::toEducation)
                .forEach(profile::addEducation);

        mapList(request.certifications(), this::toCertification)
                .forEach(profile::addCertification);

        mapList(request.languages(), this::toLanguage)
                .forEach(profile::addLanguage);

        mapList(request.skills(), this::toSkill)
                .forEach(profile::addSkill);

        mapList(request.websites(), this::toWebsite)
                .forEach(profile::addWebsite);

        mapList(request.socialNetworks(), this::toSocialNetwork)
                .forEach(profile::addSocialNetwork);

        return profile;
    }

    public CandidateProfileResponse toResponse(CandidateProfile profile) {
        PersonalInformation personalInfo = profile.getPersonalInformation();

        return new CandidateProfileResponse(
                profile.getId(),
                toPersonalInformationResponse(personalInfo),
                mapList(profile.getWorkExperiences(), this::toWorkExperienceResponse),
                mapList(profile.getEducations(), this::toEducationResponse),
                mapList(profile.getCertifications(), this::toCertificationResponse),
                mapList(profile.getLanguages(), this::toLanguageResponse),
                mapList(profile.getSkills(), this::toSkillResponse),
                mapList(profile.getWebsites(), this::toWebsiteResponse),
                mapList(profile.getSocialNetworks(), this::toSocialNetworkResponse),
                toResumeResponse(profile.getResume()),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    private PersonalInformation toPersonalInformation(
            CandidateProfileRequest.PersonalInformation request
    ) {
        PersonalInformation information = new PersonalInformation();
        information.setLegalName(toLegalName(request.legalName()));
        information.setPreferredName(toPreferredName(request.preferredName()));
        information.setCountry(request.country());
        information.setGender(request.gender());
        information.setAddress(toAddress(request.address()));
        information.setEmail(request.email());
        information.setPhone(toPhone(request.phone()));
        return information;
    }

    private CandidateProfileRequest.PersonalInformation toPersonalInformationResponse(
            PersonalInformation information
    ) {
        return new CandidateProfileRequest.PersonalInformation(
                toLegalNameResponse(information.getLegalName()),
                toPreferredNameResponse(information.getPreferredName()),
                information.getCountry(),
                information.getGender(),
                toAddressResponse(information.getAddress()),
                information.getEmail(),
                toPhoneResponse(information.getPhone())
        );
    }

    private LegalName toLegalName(CandidateProfileRequest.LegalName request) {
        if (request == null) return null;

        return new LegalName(
                request.prefix(),
                request.givenName(),
                request.familyName(),
                request.localGivenName(),
                request.localFamilyName()
        );
    }

    private CandidateProfileRequest.LegalName toLegalNameResponse(LegalName name) {
        if (name == null) return null;

        return new CandidateProfileRequest.LegalName(
                name.getPrefix(),
                name.getGivenName(),
                name.getFamilyName(),
                name.getLocalGivenName(),
                name.getLocalFamilyName()
        );
    }

    private PreferredName toPreferredName(CandidateProfileRequest.PreferredName request) {
        if (request == null) return null;

        return new PreferredName(
                request.hasPreferredName(),
                request.prefix(),
                request.givenName(),
                request.familyName(),
                request.localGivenName(),
                request.localFamilyName()
        );
    }

    private CandidateProfileRequest.PreferredName toPreferredNameResponse(PreferredName name) {
        if (name == null) return null;

        return new CandidateProfileRequest.PreferredName(
                name.isEnabled(),
                name.getPrefix(),
                name.getGivenName(),
                name.getFamilyName(),
                name.getLocalGivenName(),
                name.getLocalFamilyName()
        );
    }

    private Address toAddress(CandidateProfileRequest.Address request) {
        if (request == null) return null;

        return new Address(
                request.line1(),
                request.line2(),
                request.line3(),
                request.city(),
                request.state(),
                request.postalCode(),
                request.country()
        );
    }

    private CandidateProfileRequest.Address toAddressResponse(Address address) {
        if (address == null) return null;

        return new CandidateProfileRequest.Address(
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getAddressLine3(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry()
        );
    }

    private Phone toPhone(CandidateProfileRequest.Phone request) {
        if (request == null) return null;

        return new Phone(
                request.deviceType(),
                request.countryCode(),
                request.number(),
                request.extension()
        );
    }

    private CandidateProfileRequest.Phone toPhoneResponse(Phone phone) {
        if (phone == null) return null;

        return new CandidateProfileRequest.Phone(
                phone.getDeviceType(),
                phone.getCountryPhoneCode(),
                phone.getPhoneNumber(),
                phone.getPhoneExtension()
        );
    }

    private WorkExperience toWorkExperience(CandidateProfileRequest.WorkExperience request) {
        WorkExperience experience = new WorkExperience();
        experience.setJobTitle(request.jobTitle());
        experience.setCompany(request.company());
        experience.setLocation(request.location());
        experience.setCurrentlyWorkingHere(request.currentlyWorkingHere());
        experience.setStartDate(request.startDate());
        experience.setEndDate(request.endDate());
        experience.setRoleDescription(request.roleDescription());
        return experience;
    }

    private CandidateProfileRequest.WorkExperience toWorkExperienceResponse(
            WorkExperience experience
    ) {
        return new CandidateProfileRequest.WorkExperience(
                experience.getJobTitle(),
                experience.getCompany(),
                experience.getLocation(),
                experience.isCurrentlyWorkingHere(),
                experience.getStartDate(),
                experience.getEndDate(),
                experience.getRoleDescription()
        );
    }

    private Education toEducation(CandidateProfileRequest.Education request) {
        Education education = new Education();
        education.setSchoolOrUniversity(request.schoolOrUniversity());
        education.setDegree(request.degree());
        education.setFieldOfStudy(request.fieldOfStudy());
        education.setCgpa(request.cgpa());
        education.setStartDate(request.startDate());
        education.setEndDate(request.endDate());
        return education;
    }

    private CandidateProfileRequest.Education toEducationResponse(Education education) {
        return new CandidateProfileRequest.Education(
                education.getSchoolOrUniversity(),
                education.getDegree(),
                education.getFieldOfStudy(),
                education.getCgpa(),
                education.getStartDate(),
                education.getEndDate()
        );
    }

    private Certification toCertification(CandidateProfileRequest.Certification request) {
        Certification certification = new Certification();
        certification.setName(request.name());
        certification.setIssueDate(request.issueDate());
        certification.setExpiryDate(request.expiryDate());
        return certification;
    }

    private CandidateProfileRequest.Certification toCertificationResponse(
            Certification certification
    ) {
        return new CandidateProfileRequest.Certification(
                certification.getName(),
                certification.getIssueDate(),
                certification.getExpiryDate()
        );
    }

    private Language toLanguage(CandidateProfileRequest.Language request) {
        return Language.builder()
                .language(request.language())
                .fluent(request.fluent())
                .proficiency(request.proficiency())
                .build();
    }

    private CandidateProfileRequest.Language toLanguageResponse(Language language) {
        return new CandidateProfileRequest.Language(
                language.getLanguage(),
                language.isFluent(),
                language.getProficiency()
        );
    }

    private Skill toSkill(CandidateProfileRequest.Skill request) {
        return Skill.builder()
                .name(request.name())
                .build();
    }

    private CandidateProfileRequest.Skill toSkillResponse(Skill skill) {
        return new CandidateProfileRequest.Skill(skill.getName());
    }

    private Website toWebsite(CandidateProfileRequest.Website request) {
        return Website.builder().
                name(request.name())
                .url(request.url())
                .build();
    }

    private CandidateProfileRequest.Website toWebsiteResponse(Website website) {
        return new CandidateProfileRequest.Website(website.getName(), website.getUrl());
    }

    private SocialNetwork toSocialNetwork(CandidateProfileRequest.SocialNetwork request) {
        return SocialNetwork.builder()
                .platform(request.platform())
                .url(request.url())
                .build();
    }

    private CandidateProfileRequest.SocialNetwork toSocialNetworkResponse(
            SocialNetwork socialNetwork
    ) {
        return new CandidateProfileRequest.SocialNetwork(
                socialNetwork.getPlatform(),
                socialNetwork.getUrl()
        );
    }

    private CandidateProfileResponse.ResumeDocumentResponse toResumeResponse(
            ResumeDocument resume
    ) {
        if (resume == null) return null;

        return new CandidateProfileResponse.ResumeDocumentResponse(
                resume.getId(),
                resume.getFileName()
        );
    }

    private <S, T> List<T> mapList(
            List<S> source,
            Function<S, T> mapper
    ) {
        if (source == null) return List.of();

        return source.stream()
                .map(mapper)
                .toList();
    }

    public void updateEntity(
            CandidateProfileRequest request,
            CandidateProfile profile
    ) {
        profile.setPersonalInformation(
                toPersonalInformation(request.personalInformation())
        );

        profile.clearWorkExperiences();
        mapList(request.workExperiences(), this::toWorkExperience)
                .forEach(profile::addWorkExperience);

        profile.clearEducations();
        mapList(request.educations(), this::toEducation)
                .forEach(profile::addEducation);

        profile.clearCertifications();
        mapList(request.certifications(), this::toCertification)
                .forEach(profile::addCertification);

        profile.clearLanguages();
        mapList(request.languages(), this::toLanguage)
                .forEach(profile::addLanguage);

        profile.clearSkills();
        mapList(request.skills(), this::toSkill)
                .forEach(profile::addSkill);

        profile.clearWebsites();
        mapList(request.websites(), this::toWebsite)
                .forEach(profile::addWebsite);

        profile.clearSocialNetworks();
        mapList(request.socialNetworks(), this::toSocialNetwork)
                .forEach(profile::addSocialNetwork);
    }
}