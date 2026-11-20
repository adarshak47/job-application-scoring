package com.adarsh.job_application_scoring.application.browser;

import com.adarsh.job_application_scoring.application.browser.config.BrowserProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaywrightBrowserFactoryTest {

    @Test
    void shouldLaunchChromiumAndOpenPage() {

        BrowserProperties properties = new BrowserProperties();
        properties.setHeadless(false);

        PlaywrightBrowserFactory factory =
                new PlaywrightBrowserFactory(properties);

        try (BrowserSession session = factory.openSession()) {

            session.getPage().navigate(
                    "data:text/html,%3Chtml%3E%3Chead%3E%3Ctitle%3EJobot%20Browser%20Test%3C%2Ftitle%3E%3C%2Fhead%3E%3Cbody%3EReady%3C%2Fbody%3E%3C%2Fhtml%3E"
            );

            assertEquals(
                    "Jobot Browser Test",
                    session.getPage().title()
            );
        }
    }
}