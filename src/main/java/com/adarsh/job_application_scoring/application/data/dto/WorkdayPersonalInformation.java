package com.adarsh.job_application_scoring.application.data.dto;

public record WorkdayPersonalInformation(
        LegalNameData legalName,
        PreferredNameData preferredName,
        String country,
        String email,
        PhoneData phone,
        AddressData address
) {

    public record LegalNameData(
            String prefix,
            String givenName,
            String familyName,
            String localGivenName,
            String localFamilyName
    ) {
    }

    public record PreferredNameData(
            Boolean hasPreferredName,
            String prefix,
            String givenName,
            String familyName,
            String localGivenName,
            String localFamilyName
    ) {
    }

    public record PhoneData(
            String deviceType,
            String countryCode,
            String number,
            String extension
    ) {
    }

    public record AddressData(
            String line1,
            String line2,
            String line3,
            String city,
            String state,
            String postalCode,
            String country
    ) {
    }
}