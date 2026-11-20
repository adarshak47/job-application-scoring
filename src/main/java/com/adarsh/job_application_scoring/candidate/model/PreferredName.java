
package com.adarsh.job_application_scoring.candidate.model;

import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PreferredName {

    @Column(name = "has_preferred_name", nullable = false)
    private boolean enabled;

    @Column(name = "preferred_name_prefix", length = 30)
    private String prefix;

    @Column(name = "preferred_given_name", length = 150)
    private String givenName;

    @Column(name = "preferred_family_name", length = 150)
    private String familyName;

    @Column(name = "preferred_local_given_name", length = 150)
    private String localGivenName;

    @Column(name = "preferred_local_family_name", length = 150)
    private String localFamilyName;
}
