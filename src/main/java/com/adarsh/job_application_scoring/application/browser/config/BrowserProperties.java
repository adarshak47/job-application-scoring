package com.adarsh.job_application_scoring.application.browser.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "jobot.browser")
public class BrowserProperties {

    private boolean headless = true;

    private double slowMo = 0;

    private double timeoutMs = 30_000;

    private double navigationTimeoutMs = 60_000;

    private int viewportWidth = 1440;

    private int viewportHeight = 900;
}