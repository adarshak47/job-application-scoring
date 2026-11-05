package com.adarsh.job_application_scoring.jobdiscovery.service;

import com.adarsh.job_application_scoring.jobdiscovery.client.WorkdayClient;
import com.adarsh.job_application_scoring.jobdiscovery.dto.WorkdayJob;
import com.adarsh.job_application_scoring.jobdiscovery.dto.WorkdayJobDiscoveryRequest;
import com.adarsh.job_application_scoring.jobdiscovery.dto.WorkdayJobDiscoveryResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WorkdayJobDiscoveryService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGES = 1_000;
    private static final String DEFAULT_LOCALE = "en-US";
    private static final Set<String> WORKDAY_DOMAINS = Set.of(
            ".myworkdayjobs.com",
            ".myworkdaysite.com"
    );
    // Recognized Workday requisition ID formats.
    private static final Pattern JOB_ID_PATTERN = Pattern.compile(
            "^((?:"
                    + "REF\\d+[A-Z]?"
                    + "|REQ-\\d+"
                    + "|JREQ\\d+"
                    + "|JR\\d+"
                    + "|R[.-]?\\d+"
                    + "|\\d{5,}"
                    + "))(?:-\\d+)?$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern POSTED_DAYS_AGO_PATTERN = Pattern.compile(
            "^Posted\\s+(\\d+)\\s+Days?\\s+Ago$",
            Pattern.CASE_INSENSITIVE
    );

    private final WorkdayClient workdayClient;

    public WorkdayJobDiscoveryService(WorkdayClient workdayClient) {
        this.workdayClient = workdayClient;
    }

    public WorkdayJobDiscoveryResponse discoverJobs(
            WorkdayJobDiscoveryRequest request
    ) {
        URI sourceUri = validateSourceUrl(request.sourceUrl());
        String sourceUrl = sourceUri.toString();

        int pageSize = request.pageSize() == null
                ? DEFAULT_PAGE_SIZE
                : request.pageSize();

        List<String> includedTitles = request.includeTitles() == null
                ? List.of()
                : request.includeTitles().stream()
                .filter(this::hasText)
                .map(this::normalize)
                .filter(title -> !title.isEmpty())
                .distinct()
                .toList();

        /*
         * Fetch once without filters to obtain Workday's facet metadata.
         * This response can also be reused as page 1 when no filters are
         * requested.
         */
        JsonNode unfilteredResponse = workdayClient.fetchJobs(
                sourceUrl,
                Map.of(),
                pageSize,
                0
        );

        Map<String, List<String>> appliedFacets = resolveAppliedFacets(
                unfilteredResponse.path("facets"),
                request.country(),
                request.location()
        );

        JsonNode firstResponse = appliedFacets.isEmpty()
                ? unfilteredResponse
                : workdayClient.fetchJobs(
                sourceUrl,
                appliedFacets,
                pageSize,
                0
        );

        Map<String, WorkdayJob> uniqueJobs = new LinkedHashMap<>();

        int totalJobs = 0;
        int offset = 0;
        int pagesFetched = 0;

        JsonNode response = firstResponse;

        while (pagesFetched < MAX_PAGES) {
            JsonNode postings = response.path("jobPostings");

            if (!postings.isArray()) {
                throw new IllegalStateException(
                        "Workday response does not contain a jobPostings array"
                );
            }

            if (pagesFetched == 0) {
                totalJobs = response.path("total").asInt(-1);

                if (totalJobs < 0) {
                    throw new IllegalStateException(
                            "Workday response does not contain a valid total"
                    );
                }
            }

            pagesFetched++;

            if (postings.isEmpty()) {
                break;
            }

            int fetchedFromPage = 0;

            for (JsonNode posting : postings) {
                WorkdayJob job = toWorkdayJob(posting, sourceUri);

                if (job.jobUrl() != null
                        && !job.jobUrl().isBlank()
                        && matchesIncludedTitle(job.title(), includedTitles)
                        && matchesPostedWithinDays(
                        job.postedOn(),
                        request.postedWithinDays()
                )) {

                    String key = job.jobId() != null
                            && !job.jobId().isBlank()
                            ? job.jobId()
                            : job.jobUrl();

                    uniqueJobs.putIfAbsent(key, job);
                }

                fetchedFromPage++;
            }

            if (fetchedFromPage == 0) {
                break;
            }

            offset += fetchedFromPage;

            if (offset >= totalJobs) {
                break;
            }

            response = workdayClient.fetchJobs(
                    sourceUrl,
                    appliedFacets,
                    pageSize,
                    offset
            );
        }

        if (pagesFetched == MAX_PAGES && offset < totalJobs) {
            throw new IllegalStateException(
                    "Stopped after the maximum pagination limit. "
                            + "Increase MAX_PAGES if this result size is expected."
            );
        }

        List<WorkdayJob> jobs = new ArrayList<>(uniqueJobs.values());

        return new WorkdayJobDiscoveryResponse(
                sourceUrl,
                totalJobs,
                jobs.size(),
                pagesFetched,
                jobs
        );
    }

    private Map<String, List<String>> resolveAppliedFacets(
            JsonNode facets,
            String country,
            String location
    ) {
        Map<String, List<String>> appliedFacets = new LinkedHashMap<>();

        if (hasText(country)) {
            List<FacetMatch> countryMatches = resolveFacets(
                    facets,
                    country,
                    FacetDimension.COUNTRY
            );

            addFacetMatches(appliedFacets, countryMatches);
        }

        if (hasText(location)) {
            List<FacetMatch> locationMatches = resolveFacets(
                    facets,
                    location,
                    FacetDimension.LOCATION
            );

            addFacetMatches(appliedFacets, locationMatches);
        }

        return appliedFacets;
    }

    private void addFacetMatches(
            Map<String, List<String>> appliedFacets,
            List<FacetMatch> matches
    ) {
        for (FacetMatch match : matches) {
            List<String> ids = appliedFacets.computeIfAbsent(
                    match.facetParameter(),
                    key -> new ArrayList<>()
            );

            if (!ids.contains(match.id())) {
                ids.add(match.id());
            }
        }
    }





    private List<FacetMatch> resolveFacets(
            JsonNode facets,
            String requestedValue,
            FacetDimension dimension
    ) {
        List<FacetMatch> matches = new ArrayList<>();

        collectFacetMatches(
                facets,
                requestedValue,
                dimension,
                matches
        );

        if (matches.isEmpty()) {
            throw new IllegalArgumentException(
                    "Could not find Workday "
                            + dimension.label()
                            + " facet value: "
                            + requestedValue
            );
        }

        // Remove duplicate facet parameter + ID combinations.
        Map<String, FacetMatch> uniqueMatches = new LinkedHashMap<>();

        for (FacetMatch match : matches) {
            String key = match.facetParameter() + "::" + match.id();

            uniqueMatches.putIfAbsent(key, match);
        }

        return new ArrayList<>(uniqueMatches.values());
    }

    private void collectFacetMatches(
            JsonNode node,
            String requestedValue,
            FacetDimension dimension,
            List<FacetMatch> matches
    ) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                collectFacetMatches(
                        child,
                        requestedValue,
                        dimension,
                        matches
                );
            }

            return;
        }

        if (!node.isObject()) {
            return;
        }

        String facetParameter = textValue(node, "facetParameter");
        String facetDescriptor = textValue(node, "descriptor");

        JsonNode values = node.path("values");

        if (hasText(facetParameter)
                && values.isArray()
                && isExpectedDimension(
                facetParameter,
                facetDescriptor,
                dimension
        )) {

            for (JsonNode value : values) {

                String descriptor = textValue(value, "descriptor");
                String id = textValue(value, "id");

                if (!hasText(descriptor) || !hasText(id)) {
                    continue;
                }

                if (matchesFacetValue(
                        requestedValue,
                        descriptor,
                        dimension
                )) {
                    matches.add(
                            new FacetMatch(facetParameter, id)
                    );
                }
            }
        }

        // Continue traversing nested Workday facet structures.
        node.fields().forEachRemaining(entry ->
                collectFacetMatches(
                        entry.getValue(),
                        requestedValue,
                        dimension,
                        matches
                )
        );
    }

    private boolean isExpectedDimension(
            String facetParameter,
            String facetDescriptor,
            FacetDimension dimension
    ) {
        String parameter = normalize(facetParameter);
        String descriptor = normalize(facetDescriptor);

        return switch (dimension) {

            case COUNTRY -> {
                yield parameter.contains("country")
                        || parameter.contains("countryregion")
                        || descriptor.contains("country")
                        || descriptor.contains("countryregion");
            }

            case LOCATION -> {
                yield parameter.contains("location")
                        || parameter.contains("city")
                        || parameter.contains("state")
                        || parameter.contains("province")
                        || descriptor.contains("location")
                        || descriptor.contains("city")
                        || descriptor.contains("state")
                        || descriptor.contains("province")
                        || descriptor.equals("locations");
            }
        };
    }

    private boolean matchesFacetValue(
            String requestedValue,
            String descriptor,
            FacetDimension dimension
    ) {
        String requested = normalize(requestedValue);
        String candidate = normalize(descriptor);

        if (requested.isEmpty() || candidate.isEmpty()) {
            return false;
        }

        // Exact match.
        if (requested.equals(candidate)) {
            return true;
        }

        // Flexible matching for location values.
        if (dimension == FacetDimension.LOCATION) {
            return candidate.contains(requested)
                    || requested.contains(candidate);
        }

        // Country matching.
        return candidate.contains(requested);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private WorkdayJob toWorkdayJob(JsonNode posting, URI sourceUri) {
        String title = textValue(posting, "title");
        String externalPath = textValue(posting, "externalPath");
        String location = textValue(posting, "locationsText");
        String postedOn = textValue(posting, "postedOn");

        String jobUrl = buildJobUrl(sourceUri, externalPath);
        String jobId = extractJobId(posting, externalPath);

        return new WorkdayJob(
                title,
                jobUrl,
                location,
                postedOn,
                jobId
        );
    }

    private String textValue(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);

        return value != null && value.isValueNode()
                ? value.asText()
                : null;
    }

    private String buildJobUrl(URI sourceUri, String externalPath) {
        if (externalPath == null || externalPath.isBlank()) {
            return null;
        }

        final URI externalUri;

        try {
            externalUri = URI.create(externalPath.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid Workday external job path",
                    exception
            );
        }

        String externalRawPath = externalUri.getRawPath();

        if (externalRawPath == null
                || !externalRawPath.startsWith("/job/")
                || externalUri.isAbsolute()
                || externalUri.getRawAuthority() != null
                || externalUri.getRawQuery() != null
                || externalUri.getRawFragment() != null
                || externalRawPath.contains("\\")
                || externalRawPath.contains("/../")
                || externalRawPath.endsWith("/..")
                || externalRawPath.contains("/./")
                || externalRawPath.endsWith("/.")) {
            throw new IllegalArgumentException(
                    "Invalid Workday external job path"
            );
        }

        // Expected API path: /wday/cxs/{tenant}/{site}/jobs
        String sourcePath = sourceUri.getPath();
        String[] segments = sourcePath.split("/");

        if (segments.length != 6
                || !"wday".equals(segments[1])
                || !"cxs".equals(segments[2])
                || segments[3].isBlank()
                || segments[4].isBlank()
                || !"jobs".equals(segments[5])) {
            throw new IllegalArgumentException(
                    "Invalid Workday jobs API path"
            );
        }

        String site = segments[4];

        // Construct the public careers URL.
        String basePath = "/" + DEFAULT_LOCALE + "/" + site;
        String jobPath = basePath + externalRawPath;

        StringBuilder result = new StringBuilder()
                .append(sourceUri.getScheme())
                .append("://")
                .append(sourceUri.getRawAuthority())
                .append(jobPath);

        // Preserve original query parameters, including repeated keys.
        if (sourceUri.getRawQuery() != null) {
            result.append('?').append(sourceUri.getRawQuery());
        }

        final URI jobUri;

        try {
            jobUri = URI.create(result.toString());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Failed to construct Workday job URL",
                    exception
            );
        }

        // Ensure the final URL stays on the original HTTPS host and port.
        if (!"https".equalsIgnoreCase(jobUri.getScheme())
                || !sourceUri.getHost().equalsIgnoreCase(jobUri.getHost())
                || jobUri.getPort() != sourceUri.getPort()
                || jobUri.getRawUserInfo() != null
                || jobUri.getRawFragment() != null) {
            throw new IllegalStateException(
                    "Workday returned a job URL outside the source host"
            );
        }

        return jobUri.toString();
    }


    private URI validateSourceUrl(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "Workday source URL must not be blank"
            );
        }

        final URI uri;

        try {
            uri = URI.create(sourceUrl.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid Workday source URL",
                    exception
            );
        }

        String host = uri.getHost();
        String path = uri.getPath();

        boolean validDomain = host != null
                && WORKDAY_DOMAINS.stream().anyMatch(domain ->
                host.toLowerCase(Locale.ROOT).endsWith(domain)
        );

        boolean validPath = path != null && (
                path.matches("/wday/cxs/[^/]+/[^/]+/jobs/?")
                        || path.matches("/recruiting/wf/[^/]+/?")
        );

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !validDomain
                || !validPath) {
            throw new IllegalArgumentException(
                    "Provide a valid HTTPS Workday jobs URL"
            );
        }

        if (uri.getUserInfo() != null || uri.getPort() != -1) {
            throw new IllegalArgumentException(
                    "Credentials and custom ports are not allowed "
                            + "in the source URL"
            );
        }

        return uri;
    }

    private enum FacetDimension {
        COUNTRY("country"),
        LOCATION("location");

        private final String label;

        FacetDimension(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private record FacetMatch(
            String facetParameter,
            String id
    ) {
    }

    private String extractJobId(JsonNode posting, String externalPath) {

        // Priority 1: Extract from externalPath.
        String jobId = extractJobIdFromExternalPath(externalPath);

        if (jobId != null) {
            return jobId;
        }

        // Priority 2: Search all bulletFields.
        JsonNode bulletFields = posting.path("bulletFields");

        if (bulletFields.isArray()) {
            for (JsonNode field : bulletFields) {

                if (!field.isTextual()) {
                    continue;
                }

                String candidate = field.asText().trim();

                Matcher matcher = JOB_ID_PATTERN.matcher(candidate);

                if (matcher.matches()) {
                    return matcher.group(1);
                }
            }
        }

        // No reliable job ID found.
        return null;
    }

    private String extractJobIdFromExternalPath(String externalPath) {

        if (externalPath == null || externalPath.isBlank()) {
            return null;
        }

        String path = externalPath.trim();

        // Get the final path segment.
        int lastSlash = path.lastIndexOf('/');

        if (lastSlash >= 0) {
            path = path.substring(lastSlash + 1);
        }

        // Job ID is expected after the final underscore.
        int lastUnderscore = path.lastIndexOf('_');

        if (lastUnderscore < 0 || lastUnderscore == path.length() - 1) {
            return null;
        }

        String candidate = path.substring(lastUnderscore + 1);

        Matcher matcher = JOB_ID_PATTERN.matcher(candidate);

        if (matcher.matches()) {
            return matcher.group(1);
        }

        return null;
    }

    private boolean matchesIncludedTitle(
            String jobTitle,
            List<String> includedTitles
    ) {
        // No title filter means all titles are accepted.
        if (includedTitles.isEmpty()) {
            return true;
        }

        if (!hasText(jobTitle)) {
            return false;
        }

        String normalizedJobTitle = normalize(jobTitle);

        return includedTitles.stream()
                .anyMatch(normalizedJobTitle::contains);
    }

    private boolean matchesPostedWithinDays(
            String postedOn,
            Integer postedWithinDays
    ) {
        // No date filter requested.
        if (postedWithinDays == null) {
            return true;
        }

        // Missing or blank posting date: include the job.
        if (!hasText(postedOn)) {
            return true;
        }

        String normalizedPostedOn = postedOn.trim();

        // Posted Today.
        if (normalizedPostedOn.equalsIgnoreCase("Posted Today")) {
            return true;
        }

        // Posted Yesterday.
        if (normalizedPostedOn.equalsIgnoreCase("Posted Yesterday")) {
            return postedWithinDays >= 1;
        }

        // Posted 30+ Days Ago.
        if (normalizedPostedOn.equalsIgnoreCase("Posted 30+ Days Ago")) {
            return postedWithinDays > 30;
        }

        // Posted N Days Ago.
        Matcher matcher = POSTED_DAYS_AGO_PATTERN.matcher(normalizedPostedOn);

        if (matcher.matches()) {
            int daysAgo = Integer.parseInt(matcher.group(1));

            return daysAgo <= postedWithinDays;
        }

        // Unknown format: include the job rather than risk excluding it.
        return true;
    }

}