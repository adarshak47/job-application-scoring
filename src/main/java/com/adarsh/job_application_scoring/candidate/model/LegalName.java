
package com.adarsh.job_application_scoring.candidate.model;

import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LegalName {

    @Column(name = "legal_name_prefix", length = 30)
    private String prefix;

    @Column(name = "legal_given_name", nullable = false, length = 150)
    private String givenName;

    @Column(name = "legal_family_name", nullable = false, length = 150)
    private String familyName;

    @Column(name = "legal_local_given_name", length = 150)
    private String localGivenName;

    @Column(name = "legal_local_family_name", length = 150)
    private String localFamilyName;
}
