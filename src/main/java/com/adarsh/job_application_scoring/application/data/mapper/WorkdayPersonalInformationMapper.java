package com.adarsh.job_application_scoring.application.data.mapper;

import com.adarsh.job_application_scoring.application.data.dto.WorkdayPersonalInformation;
import com.adarsh.job_application_scoring.candidate.model.Address;
import com.adarsh.job_application_scoring.candidate.model.CandidateProfile;
import com.adarsh.job_application_scoring.candidate.model.LegalName;
import com.adarsh.job_application_scoring.candidate.model.PersonalInformation;
import com.adarsh.job_application_scoring.candidate.model.Phone;
import com.adarsh.job_application_scoring.candidate.model.PreferredName;
import org.springframework.stereotype.Component;

@Component
public class WorkdayPersonalInformationMapper {

    public WorkdayPersonalInformation toDto(CandidateProfile candidateProfile) {

        if (candidateProfile == null ||
                candidateProfile.getPersonalInformation() == null) {
            return null;
        }

        PersonalInformation personalInformation =
                candidateProfile.getPersonalInformation();

        return new WorkdayPersonalInformation(
                mapLegalName(personalInformation.getLegalName()),
                mapPreferredName(personalInformation.getPreferredName()),
                personalInformation.getCountry(),
                personalInformation.getEmail(),
                mapPhone(personalInformation.getPhone()),
                mapAddress(personalInformation.getAddress())
        );
    }

    private WorkdayPersonalInformation.LegalNameData mapLegalName(
            LegalName legalName) {

        if (legalName == null) {
            return null;
        }

        return new WorkdayPersonalInformation.LegalNameData(
                legalName.getPrefix(),
                legalName.getGivenName(),
                legalName.getFamilyName(),
                legalName.getLocalGivenName(),
                legalName.getLocalFamilyName()
        );
    }

    private WorkdayPersonalInformation.PreferredNameData mapPreferredName(
            PreferredName preferredName) {

        if (preferredName == null) {
            return null;
        }

        return new WorkdayPersonalInformation.PreferredNameData(
                preferredName.isEnabled(),
                preferredName.getPrefix(),
                preferredName.getGivenName(),
                preferredName.getFamilyName(),
                preferredName.getLocalGivenName(),
                preferredName.getLocalFamilyName()
        );
    }

    private WorkdayPersonalInformation.PhoneData mapPhone(Phone phone) {

        if (phone == null) {
            return null;
        }

        return new WorkdayPersonalInformation.PhoneData(
                phone.getDeviceType(),
                phone.getCountryPhoneCode(),
                phone.getPhoneNumber(),
                phone.getPhoneExtension()
        );
    }

    private WorkdayPersonalInformation.AddressData mapAddress(Address address) {

        if (address == null) {
            return null;
        }

        return new WorkdayPersonalInformation.AddressData(
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getAddressLine3(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry()
        );
    }
}