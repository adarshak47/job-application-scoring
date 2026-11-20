
package com.adarsh.job_application_scoring.application.data.mapper;

import com.adarsh.job_application_scoring.application.data.dto.WorkdayResumeData;
import com.adarsh.job_application_scoring.candidate.model.CandidateProfile;
import com.adarsh.job_application_scoring.candidate.model.ResumeDocument;
import com.adarsh.job_application_scoring.candidate.service.ResumeStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkdayResumeMapper {

    private final ResumeStorageService resumeStorageService;

    public WorkdayResumeData toDto(CandidateProfile candidateProfile) {

        if (candidateProfile == null ||
                candidateProfile.getResume() == null) {
            return null;
        }

        ResumeDocument resumeDocument = candidateProfile.getResume();

        return new WorkdayResumeData(
                resumeDocument.getFileName(),
                resumeStorageService.resolvePath(
                        resumeDocument.getFilePath()
                )
        );
    }
}
