
package com.adarsh.job_application_scoring.application.browser.service;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import org.springframework.stereotype.Component;

@Component
public class WorkdayPageRenderWaitHandler {

    private static final int NETWORK_IDLE_TIMEOUT_MS = 5000;
    private static final int PAGE_RENDER_TIMEOUT_MS = 20000;
    private static final int DOM_STABILITY_PERIOD_MS = 1500;

    /**
     * Waits for the initial document load, gives network requests
     * an opportunity to finish, and then waits for DOM stability.
     */
    public void waitForPageRendered(Page page) {

        // Step 1: Wait for the initial document to be parsed.
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Step 2: Wait for network activity to settle.
        // Workday may maintain background requests, so this is best-effort.
        try {
            page.waitForLoadState(
                    LoadState.NETWORKIDLE,
                    new Page.WaitForLoadStateOptions()
                            .setTimeout(NETWORK_IDLE_TIMEOUT_MS)
            );
        } catch (Exception ignored) {
            // Continue with DOM-based synchronization.
        }

        // Step 3: Wait until the document is completely loaded
        // and meaningful content remains stable.
        try {

            page.waitForFunction(
                    "() => new Promise(resolve => {" +

                            "  const quietPeriod = " + DOM_STABILITY_PERIOD_MS + ";" +

                            "  let timer = null;" +

                            "  const isReady = () => {" +
                            "    const body = document.body;" +
                            "    if (!body || document.readyState !== 'complete') {" +
                            "      return false;" +
                            "    }" +

                            "    const text = (body.innerText || '').trim();" +
                            "    const hasControls = body.querySelector(" +
                            "      'input, button, select, textarea, " +
                            "       [data-automation-id], h1, h2'" +
                            "    ) !== null;" +

                            "    return text.length > 0 || hasControls;" +
                            "  };" +

                            "  const observer = new MutationObserver(() => {" +
                            "    checkReadiness();" +
                            "  });" +

                            "  const checkReadiness = () => {" +
                            "    if (!isReady()) {" +
                            "      if (timer !== null) {" +
                            "        clearTimeout(timer);" +
                            "        timer = null;" +
                            "      }" +
                            "      return;" +
                            "    }" +

                            "    if (timer !== null) {" +
                            "      clearTimeout(timer);" +
                            "    }" +

                            "    timer = setTimeout(() => {" +
                            "      observer.disconnect();" +
                            "      resolve(true);" +
                            "    }, quietPeriod);" +
                            "  };" +

                            "  observer.observe(document.documentElement, {" +
                            "    childList: true," +
                            "    subtree: true," +
                            "    attributes: true," +
                            "    characterData: true" +
                            "  });" +

                            "  checkReadiness();" +
                            "})",

                    null,

                    new Page.WaitForFunctionOptions()
                            .setTimeout(PAGE_RENDER_TIMEOUT_MS)
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Workday page did not finish rendering within "
                            + PAGE_RENDER_TIMEOUT_MS + " ms",
                    e
            );
        }
    }
}
