package com.adarsh.job_application_scoring.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.ai.gemini")
public class GeminiProperties {

    private String apiKey;
    private String baseUrl;
    private String model;
    private Duration connectTimeout;
    private Duration readTimeout;

}
