package com.adarsh.job_application_scoring.application.data.validator;

import com.adarsh.job_application_scoring.application.data.dto.CandidateDataReadiness;
import com.adarsh.job_application_scoring.application.data.dto.WorkdayCandidateCollections;
import com.adarsh.job_application_scoring.application.data.dto.WorkdayPersonalInformation;
import com.adarsh.job_application_scoring.application.data.dto.WorkdayResumeData;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CandidateDataReadinessValidator {

    public CandidateDataReadiness validate(
            WorkdayPersonalInformation personalInformation,
            WorkdayCandidateCollections collections,
            WorkdayResumeData resume) {

        List<String> availableFields = new ArrayList<>();
        List<String> missingFields = new ArrayList<>();
        List<String> emptyCollections = new ArrayList<>();

        validatePersonalInformation(
                personalInformation,
                availableFields,
                missingFields
        );

        validateCollections(
                collections,
                availableFields,
                emptyCollections
        );

        boolean resumeAvailable = resume != null
                && resume.filePath() != null;

        addResult(
                "resume",
                resumeAvailable,
                availableFields,
                missingFields
        );

        return new CandidateDataReadiness(
                List.copyOf(availableFields),
                List.copyOf(missingFields),
                List.copyOf(emptyCollections),
                resumeAvailable
        );
    }

    private void validatePersonalInformation(
            WorkdayPersonalInformation information,
            List<String> availableFields,
            List<String> missingFields) {

        if (information == null) {
            missingFields.add("personalInformation");
            return;
        }

        WorkdayPersonalInformation.LegalNameData legalName =
                information.legalName();

        addResult(
                "legalName.givenName",
                legalName != null && hasText(legalName.givenName()),
                availableFields,
                missingFields
        );

        addResult(
                "legalName.familyName",
                legalName != null && hasText(legalName.familyName()),
                availableFields,
                missingFields
        );

        addResult(
                "country",
                hasText(information.country()),
                availableFields,
                missingFields
        );

        addResult(
                "email",
                hasText(information.email()),
                availableFields,
                missingFields
        );

        WorkdayPersonalInformation.PhoneData phone =
                information.phone();

        addResult(
                "phone.number",
                phone != null && hasText(phone.number()),
                availableFields,
                missingFields
        );

        WorkdayPersonalInformation.AddressData address =
                information.address();

        addResult(
                "address.city",
                address != null && hasText(address.city()),
                availableFields,
                missingFields
        );

        addResult(
                "address.country",
                address != null && hasText(address.country()),
                availableFields,
                missingFields
        );
    }

    private void validateCollections(
            WorkdayCandidateCollections collections,
            List<String> availableFields,
            List<String> emptyCollections) {

        if (collections == null) {
            emptyCollections.add("allCandidateCollections");
            return;
        }

        checkCollection(
                "workExperiences",
                collections.workExperiences(),
                availableFields,
                emptyCollections
        );

        checkCollection(
                "educations",
                collections.educations(),
                availableFields,
                emptyCollections
        );

        checkCollection(
                "certifications",
                collections.certifications(),
                availableFields,
                emptyCollections
        );

        checkCollection(
                "languages",
                collections.languages(),
                availableFields,
                emptyCollections
        );

        checkCollection(
                "skills",
                collections.skills(),
                availableFields,
                emptyCollections
        );

        checkCollection(
                "websites",
                collections.websites(),
                availableFields,
                emptyCollections
        );

        checkCollection(
                "socialNetworks",
                collections.socialNetworks(),
                availableFields,
                emptyCollections
        );
    }

    private void checkCollection(
            String fieldName,
            List<?> values,
            List<String> availableFields,
            List<String> emptyCollections) {

        if (values == null || values.isEmpty()) {
            emptyCollections.add(fieldName);
        } else {
            availableFields.add(fieldName);
        }
    }

    private void addResult(
            String fieldName,
            boolean present,
            List<String> availableFields,
            List<String> missingFields) {

        if (present) {
            availableFields.add(fieldName);
        } else {
            missingFields.add(fieldName);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}