package com.adarsh.job_application_scoring.application.browser.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "jobot.workday.login")
public class WorkdayLoginProperties {

    private String email;

    private String password;
}