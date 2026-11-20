package com.adarsh.job_application_scoring.candidate.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jobot.storage")
public class ResumeStorageProperties {

    private Path resumeDirectory = Path.of("./data/resumes");
}