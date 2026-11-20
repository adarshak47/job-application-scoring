
package com.adarsh.job_application_scoring.candidate.model;

import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonalInformation {

    @Embedded
    private LegalName legalName;

    @Embedded
    private PreferredName preferredName;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(length = 30)
    private String gender;

    @Embedded
    private Address address;

    @Column(nullable = false)
    private String email;

    @Embedded
    private Phone phone;
}
