package com.adarsh.job_application_scoring.jobdiscovery.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class WorkdayClient {

    private final RestClient restClient;

    public WorkdayClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public JsonNode fetchJobs(
            String sourceUrl,
            Map<String, List<String>> appliedFacets,
            int limit,
            int offset
    ) {
        Map<String, Object> requestBody = Map.of(
                "appliedFacets", appliedFacets,
                "limit", limit,
                "offset", offset,
                "searchText", ""
        );

        JsonNode response = restClient.post()
                .uri(sourceUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.isObject()) {
            throw new IllegalStateException(
                    "Workday returned an empty or invalid JSON response"
            );
        }

        return response;
    }
}