package com.adarsh.job_application_scoring.jobdescription.service;

import com.adarsh.job_application_scoring.jobdescription.extractor.JobDescriptionExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class JobDescriptionExtractionService {

    private static final int MINIMUM_JOB_DESCRIPTION_LENGTH = 200;

    private final JobDescriptionExtractor jobDescriptionExtractor;

    public String extractJobDescription(String jobUrl) {
        String extractedText = jobDescriptionExtractor.extract(jobUrl);

        if (extractedText == null
                || extractedText.isBlank()
                || extractedText.length() < MINIMUM_JOB_DESCRIPTION_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Could not extract enough job description text from this URL. "
                            + "The page may require JavaScript, authentication, or a different URL."
            );
        }

        return extractedText;
    }
}
