package com.adarsh.job_application_scoring.application.browser;

import com.adarsh.job_application_scoring.application.browser.config.BrowserProperties;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import org.springframework.stereotype.Component;

@Component
public class PlaywrightBrowserFactory {

    private final BrowserProperties properties;

    public PlaywrightBrowserFactory(BrowserProperties properties) {
        this.properties = properties;
    }

    public BrowserSession openSession() {

        Playwright playwright = Playwright.create();

        try {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(properties.isHeadless())
                            .setSlowMo(properties.getSlowMo())
            );

            try {
                BrowserContext context = browser.newContext(
                        new Browser.NewContextOptions()
                                .setViewportSize(
                                        properties.getViewportWidth(),
                                        properties.getViewportHeight()
                                )
                );

                try {
                    var page = context.newPage();

                    page.setDefaultTimeout(properties.getTimeoutMs());
                    page.setDefaultNavigationTimeout(
                            properties.getNavigationTimeoutMs()
                    );

                    return new BrowserSession(
                            playwright,
                            browser,
                            context,
                            page
                    );

                } catch (RuntimeException exception) {
                    context.close();
                    throw exception;
                }

            } catch (RuntimeException exception) {
                browser.close();
                throw exception;
            }

        } catch (RuntimeException exception) {
            playwright.close();
            throw exception;
        }
    }
}