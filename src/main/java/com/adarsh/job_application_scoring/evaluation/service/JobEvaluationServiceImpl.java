
package com.adarsh.job_application_scoring.evaluation.service;

import com.adarsh.job_application_scoring.ai.client.AiEvaluationClient;
import com.adarsh.job_application_scoring.ai.dto.GeminiAssessmentPayload;
import com.adarsh.job_application_scoring.evaluation.dto.request.EvaluateJobRequest;
import com.adarsh.job_application_scoring.evaluation.dto.response.EvaluationVerdict;
import com.adarsh.job_application_scoring.evaluation.dto.response.JobEvaluationResponse;
import com.adarsh.job_application_scoring.evaluation.dto.response.RequirementAssessment;
import com.adarsh.job_application_scoring.evaluation.dto.response.RequirementStatus;
import com.adarsh.job_application_scoring.evaluation.dto.response.RequirementType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class JobEvaluationServiceImpl implements JobEvaluationService {

    private final AiEvaluationClient aiEvaluationClient;

    public JobEvaluationServiceImpl(AiEvaluationClient aiEvaluationClient) {
        this.aiEvaluationClient = aiEvaluationClient;
    }

    @Override
    public JobEvaluationResponse evaluate(EvaluateJobRequest request) {
        GeminiAssessmentPayload payload = aiEvaluationClient.assess(request);
        validatePayload(payload);

        List<RequirementAssessment> assessments = payload.requirementAssessments();

        int score = calculateScore(assessments);
        EvaluationVerdict verdict = determineVerdict(score, assessments);

        List<String> risks = new ArrayList<>(payload.risks());

        assessments.stream()
                .filter(assessment ->
                        assessment.requirementType() == RequirementType.REQUIRED
                                && assessment.requirementStatus() == RequirementStatus.MISSING
                )
                .map(assessment ->
                        "Missing evidence for required qualification: "
                                + assessment.requirement()
                )
                .filter(risk -> !risks.contains(risk))
                .forEach(risks::add);

        return new JobEvaluationResponse(
                score,
                verdict,
                assessments,
                payload.strengths(),
                risks,
                buildRecommendation(score, verdict, assessments)
        );
    }

    private void validatePayload(GeminiAssessmentPayload payload) {
        if (payload == null
                || payload.requirementAssessments() == null
                || payload.requirementAssessments().isEmpty()
                || payload.strengths() == null
                || payload.risks() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "The AI provider returned an incomplete evaluation"
            );
        }

        if (payload.requirementAssessments().size() > 20) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "The AI provider returned too many requirement assessments"
            );
        }

        boolean invalidAssessment = payload.requirementAssessments().stream()
                .anyMatch(assessment ->
                        assessment == null
                                || isBlank(assessment.requirement())
                                || assessment.requirementType() == null
                                || assessment.requirementStatus() == null
                                || isBlank(assessment.resumeEvidence())
                                || isBlank(assessment.explanation())
                );

        if (invalidAssessment) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "The AI provider returned an invalid requirement assessment"
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private int calculateScore(List<RequirementAssessment> assessments) {
        double totalWeight = 0;
        double earnedWeight = 0;

        for (RequirementAssessment assessment : assessments) {
            double weight = assessment.requirementType() == RequirementType.REQUIRED
                    ? 2.0
                    : 1.0;

            totalWeight += weight;

            earnedWeight += switch (assessment.requirementStatus()) {
                case MATCHED -> weight;
                case PARTIALLY_MATCHED -> weight * 0.5;
                case MISSING -> 0.0;
            };
        }

        if (totalWeight == 0) {
            return 0;
        }

        return (int) Math.round((earnedWeight / totalWeight) * 100);
    }

    private EvaluationVerdict determineVerdict(
            int score,
            List<RequirementAssessment> assessments
    ) {
        boolean missingRequiredRequirement = assessments.stream()
                .anyMatch(assessment ->
                        assessment.requirementType() == RequirementType.REQUIRED
                                && assessment.requirementStatus() == RequirementStatus.MISSING
                );

        if (score >= 75 && !missingRequiredRequirement) {
            return EvaluationVerdict.APPLY;
        }

        if (score >= 45) {
            return EvaluationVerdict.APPLY_WITH_RESERVATIONS;
        }

        return EvaluationVerdict.LOW_PRIORITY;
    }

    private String buildRecommendation(
            int score,
            EvaluationVerdict verdict,
            List<RequirementAssessment> assessments
    ) {
        long matched = assessments.stream()
                .filter(assessment ->
                        assessment.requirementStatus() == RequirementStatus.MATCHED
                )
                .count();

        long required = assessments.stream()
                .filter(assessment ->
                        assessment.requirementType() == RequirementType.REQUIRED
                )
                .count();

        return switch (verdict) {
            case APPLY -> "The resume demonstrates a strong match with the "
                    + "assessed requirements. "
                    + matched + " requirement(s) were fully matched. "
                    + "Verify the evidence before applying.";

            case APPLY_WITH_RESERVATIONS -> "The match score is " + score
                    + "/100. Review the identified gaps and confirm whether "
                    + "any missing required qualifications are deal-breakers. "
                    + required + " requirement(s) were classified as required.";

            case LOW_PRIORITY -> "The resume shows limited demonstrated "
                    + "alignment with this role. Review the assessments to "
                    + "check whether the apparent gaps are genuine before "
                    + "deciding not to apply.";
        };
    }
}
