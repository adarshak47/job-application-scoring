
package com.adarsh.job_application_scoring.candidate.model;

import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Phone {

    @Column(name = "phone_device_type", length = 30)
    private String deviceType;

    @Column(name = "phone_country_code", length = 10)
    private String countryPhoneCode;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "phone_extension", length = 15)
    private String phoneExtension;
}
