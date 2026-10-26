package com.adarsh.job_application_scoring.jobdescription.extractor;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;

@Component
public class JsoupJobDescriptionExtractor implements JobDescriptionExtractor{

    private static final int CONNECTION_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;
    private static final int MAX_RESPONSE_SIZE_BYTES = 2 * 1024 * 1024;
    private static final int MAX_REDIRECTS = 5;

    private static final String USER_AGENT =
            "Mozilla/5.0 (compatible; JobotJobDescriptionExtractor/1.0)";

    private static final List<String> JOB_DESCRIPTION_SELECTORS = List.of(
            "[data-testid*=job-description]",
            "[data-testid*=jobDescription]",
            "#job-description",
            "#jobDescription",
            "[class*=job-description]",
            "[class*=jobDescription]",
            "[id*=job-description]",
            "[class*=description]",
            "article",
            "main"
    );

    @Override
    public String extract(String jobUrl) {
        URI currentUri = validateAndNormalizeUrl(jobUrl);

        for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
            validatePublicDestination(currentUri);

            Connection.Response response;

            try {
                response = Jsoup.connect(currentUri.toString())
                        .userAgent(USER_AGENT)
                        .timeout(CONNECTION_TIMEOUT_MS + READ_TIMEOUT_MS)
                        .maxBodySize(MAX_RESPONSE_SIZE_BYTES)
                        .followRedirects(false)
                        .ignoreHttpErrors(true)
                        .ignoreContentType(true)
                        .header("Accept", "text/html,application/xhtml+xml")
                        .execute();
            } catch (IOException exception) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Unable to retrieve the job page.",
                        exception
                );
            }

            int statusCode = response.statusCode();

            if (isRedirect(statusCode)) {
                String location = response.header("Location");

                if (location == null || location.isBlank()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_GATEWAY,
                            "The job page returned an invalid redirect."
                    );
                }

                if (redirectCount == MAX_REDIRECTS) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_GATEWAY,
                            "The job page exceeded the maximum redirect limit."
                    );
                }

                try {
                    currentUri = validateAndNormalizeUrl(
                            currentUri.resolve(location).toString()
                    );
                } catch (ResponseStatusException exception) {
                    throw exception;
                }

                continue;
            }

            if (statusCode < 200 || statusCode >= 300) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "The job website returned HTTP status " + statusCode + "."
                );
            }

            String contentType = response.contentType();

            if (contentType == null
                    || !(contentType.toLowerCase().contains("text/html")
                    || contentType.toLowerCase().contains("application/xhtml+xml"))) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "The supplied URL does not return an HTML page."
                );
            }

            Document document = Jsoup.parse(response.body(), currentUri.toString());

            removeUnwantedElements(document);

            return extractRelevantText(document);
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Unable to retrieve the job page."
        );
    }

    private URI validateAndNormalizeUrl(String jobUrl) {
        if (jobUrl == null || jobUrl.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Job URL must not be empty."
            );
        }

        final URI uri;

        try {
            uri = URI.create(jobUrl.trim()).normalize();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid job URL.",
                    exception
            );
        }

        String scheme = uri.getScheme();

        if (scheme == null
                || !(scheme.equalsIgnoreCase("http")
                || scheme.equalsIgnoreCase("https"))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only HTTP and HTTPS job URLs are supported."
            );
        }

        if (uri.getHost() == null
                || uri.getHost().isBlank()
                || uri.getRawUserInfo() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The job URL must contain a valid hostname and must not contain credentials."
            );
        }

        if (uri.getPort() != -1 && (uri.getPort() < 1 || uri.getPort() > 65535)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid port in job URL."
            );
        }

        return uri;
    }

    private void validatePublicDestination(URI uri) {
        String host = uri.getHost();

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);

            if (addresses.length == 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "The job URL hostname could not be resolved."
                );
            }

            for (InetAddress address : addresses) {
                if (isNonPublicAddress(address)) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "The job URL must resolve only to publicly routable IP addresses."
                    );
                }
            }
        } catch (UnknownHostException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The job URL hostname could not be resolved.",
                    exception
            );
        }
    }

    private boolean isNonPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();

        if (address instanceof java.net.Inet4Address) {
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;

            // Unspecified and current-network addresses: 0.0.0.0/8.
            if (first == 0) {
                return true;
            }

            // Carrier-grade NAT: 100.64.0.0/10.
            if (first == 100 && second >= 64 && second <= 127) {
                return true;
            }

            // Documentation ranges.
            if (first == 192 && second == 0 && (bytes[2] & 0xFF) == 2) {
                return true;
            }

            if (first == 192 && second == 88 && (bytes[2] & 0xFF) == 99) {
                return true;
            }

            if (first == 198 && (second == 18 || second == 19)) {
                return true;
            }

            if (first == 198 && second == 51 && (bytes[2] & 0xFF) == 100) {
                return true;
            }

            if (first == 203 && second == 0 && (bytes[2] & 0xFF) == 113) {
                return true;
            }

            return first >= 224;
        }

        if (address instanceof java.net.Inet6Address) {
            // Unique-local IPv6: fc00::/7.
            if ((bytes[0] & 0xFE) == 0xFC) {
                return true;
            }

            // IPv6 documentation range: 2001:db8::/32.
            if ((bytes[0] & 0xFF) == 0x20
                    && (bytes[1] & 0xFF) == 0x01
                    && (bytes[2] & 0xFF) == 0x0D
                    && (bytes[3] & 0xFF) == 0xB8) {
                return true;
            }
        }

        return false;
    }

    private boolean isRedirect(int statusCode) {
        return statusCode == 301
                || statusCode == 302
                || statusCode == 303
                || statusCode == 307
                || statusCode == 308;
    }

    private void removeUnwantedElements(Document document) {
        document.select(
                "script, style, nav, header, footer, aside, form, noscript, svg, iframe"
        ).remove();
    }


    private String extractRelevantText(Document document) {
        // First, try to extract the description from the actual HTML elements.
        for (String selector : JOB_DESCRIPTION_SELECTORS) {
            Element bestCandidate = null;
            int longestTextLength = 0;

            for (Element element : document.select(selector)) {
                String text = element.text().trim();

                if (text.length() > longestTextLength) {
                    longestTextLength = text.length();
                    bestCandidate = element;
                }
            }

            if (bestCandidate != null && longestTextLength >= 200) {
                return bestCandidate.text().trim();
            }
        }

        // Workday and other React-based sites often expose the description
        // in metadata even when the page body has not been rendered.
        String[] metadataSelectors = {
                "meta[property=og:description]",
                "meta[name=description]",
                "meta[name=twitter:description]"
        };

        for (String selector : metadataSelectors) {
            Element meta = document.selectFirst(selector);

            if (meta != null) {
                String description = meta.attr("content").trim();

                if (description.length() >= 200) {
                    return description;
                }
            }
        }

        // Last resort: return whatever text is available in the body.
        return document.body() == null
                ? ""
                : document.body().text().trim();
    }


}
