
package com.adarsh.job_application_scoring.evaluation.service;

import com.adarsh.job_application_scoring.ai.client.AiEvaluationClient;
import com.adarsh.job_application_scoring.ai.dto.GeminiAssessmentPayload;
import com.adarsh.job_application_scoring.evaluation.dto.request.EvaluateJobRequest;
import com.adarsh.job_application_scoring.evaluation.dto.response.EvaluationVerdict;
import com.adarsh.job_application_scoring.evaluation.dto.response.RequirementAssessment;
import com.adarsh.job_application_scoring.evaluation.dto.response.RequirementStatus;
import com.adarsh.job_application_scoring.evaluation.dto.response.RequirementType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobEvaluationServiceImplTest {

    @Mock
    private AiEvaluationClient aiEvaluationClient;

    @InjectMocks
    private JobEvaluationServiceImpl jobEvaluationService;

    @Test
    void shouldRecommendApplyWhenScoreIsHighAndNoRequiredRequirementIsMissing() {
        EvaluateJobRequest request = createRequest();

        when(aiEvaluationClient.assess(request)).thenReturn(
                new GeminiAssessmentPayload(
                        List.of(
                                assessment("Java", RequirementType.REQUIRED, RequirementStatus.MATCHED),
                                assessment("Spring Boot", RequirementType.REQUIRED, RequirementStatus.MATCHED),
                                assessment("SQL", RequirementType.PREFERRED, RequirementStatus.MISSING)
                        ),
                        List.of("Relevant Java backend experience"),
                        List.of("SQL evidence is missing")
                )
        );

        var response = jobEvaluationService.evaluate(request);

        assertEquals(80, response.matchScore());
        assertEquals(EvaluationVerdict.APPLY, response.evaluationVerdict());
    }

    @Test
    void shouldNotRecommendApplyWhenRequiredRequirementIsMissing() {
        EvaluateJobRequest request = createRequest();

        when(aiEvaluationClient.assess(request)).thenReturn(
                new GeminiAssessmentPayload(
                        List.of(
                                assessment("Java", RequirementType.REQUIRED, RequirementStatus.MATCHED),
                                assessment("Spring Boot", RequirementType.REQUIRED, RequirementStatus.MATCHED),
                                assessment("Kafka", RequirementType.REQUIRED, RequirementStatus.MISSING),
                                assessment("Docker", RequirementType.PREFERRED, RequirementStatus.MATCHED)
                        ),
                        List.of("Java and Spring Boot experience"),
                        List.of("No Kafka evidence")
                )
        );

        var response = jobEvaluationService.evaluate(request);

        assertEquals(71, response.matchScore());
        assertEquals(
                EvaluationVerdict.APPLY_WITH_RESERVATIONS,
                response.evaluationVerdict()
        );
    }

    @Test
    void shouldRecommendLowPriorityWhenScoreIsLow() {
        EvaluateJobRequest request = createRequest();

        when(aiEvaluationClient.assess(request)).thenReturn(
                new GeminiAssessmentPayload(
                        List.of(
                                assessment("Java", RequirementType.REQUIRED, RequirementStatus.MISSING),
                                assessment("Spring Boot", RequirementType.REQUIRED, RequirementStatus.MISSING),
                                assessment("Kubernetes", RequirementType.PREFERRED, RequirementStatus.MATCHED)
                        ),
                        List.of(),
                        List.of("Required skills are not demonstrated")
                )
        );

        var response = jobEvaluationService.evaluate(request);

        assertEquals(20, response.matchScore());
        assertEquals(EvaluationVerdict.LOW_PRIORITY, response.evaluationVerdict());
    }

    private EvaluateJobRequest createRequest() {
        return new EvaluateJobRequest(
                "Java backend developer",
                "Java and Spring Boot engineer"
        );
    }

    private RequirementAssessment assessment(
            String requirement,
            RequirementType type,
            RequirementStatus status
    ) {
        String evidence = status == RequirementStatus.MISSING
                ? "No supporting evidence found in the resume."
                : "Relevant experience stated in the resume.";

        return new RequirementAssessment(
                requirement,
                type,
                status,
                evidence,
                "Assessment produced by the mocked AI client."
        );
    }
}
