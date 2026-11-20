package com.adarsh.job_application_scoring.application.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.Getter;

@Getter
public class BrowserSession implements AutoCloseable {

    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext context;
    private final Page page;

    private boolean closed;

    public BrowserSession(
            Playwright playwright,
            Browser browser,
            BrowserContext context,
            Page page) {

        this.playwright = playwright;
        this.browser = browser;
        this.context = context;
        this.page = page;
    }

    @Override
    public void close() {

        if (closed) {
            return;
        }

        closed = true;

        try {
            context.close();
        } finally {
            try {
                browser.close();
            } finally {
                playwright.close();
            }
        }
    }
}