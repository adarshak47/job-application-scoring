package com.adarsh.job_application_scoring.application.browser.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        BrowserProperties.class,
        WorkdayLoginProperties.class
})
public class BrowserConfiguration {
}