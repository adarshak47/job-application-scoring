package com.adarsh.job_application_scoring.evaluation.service;

import com.adarsh.job_application_scoring.evaluation.dto.request.EvaluateJobRequest;
import com.adarsh.job_application_scoring.evaluation.dto.response.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.adarsh.job_application_scoring.evaluation.dto.response.RequirementStatus.*;

@Service
public class JobEvaluationServiceImpl implements JobEvaluationService {

    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z0-9+#.]+");

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "been", "being",
            "by", "can", "could", "do", "for", "from", "has", "have",
            "in", "into", "is", "it", "its", "may", "must", "of", "on",
            "or", "our", "should", "that", "the", "their", "them",
            "there", "these", "this", "those", "to", "using", "use",
            "was", "we", "were", "will", "with", "would", "you", "your",
            "experience", "knowledge", "ability", "strong", "good",
            "working", "work", "skills", "skill", "including",
            "responsibilities", "requirements", "required", "preferred"
    );

    private static final int MAX_REQUIREMENTS = 20;

    public JobEvaluationResponse evaluate(EvaluateJobRequest evaluateJobRequest) {
        Set<String> resumeTerms = extractTerms(evaluateJobRequest.resumeText());
        List<String> requirements = extractRequirements(evaluateJobRequest.jobDescription());
        if(requirements.isEmpty()) {
            return new JobEvaluationResponse(0,
                    EvaluationVerdict.LOW_PRIORITY,
                    List.of(),
                    List.of(),
                    List.of("The job description did not contain enough text to assess."),
                    "Provide a more detailed job description before making an application decision.");
        }
        List<RequirementAssessment> requirementAssessments = requirements.stream().map(requirement -> assessRequirement(requirement, resumeTerms)).toList();
        int score = calculateScore(requirementAssessments);
        EvaluationVerdict evaluationVerdict = determineVerdict(score, requirementAssessments);
        List<String> strengths = requirementAssessments.stream().filter(requirementAssessment -> requirementAssessment.requirementStatus() == MATCHED)
                .map(requirementAssessment -> "Potential keyword match: " + requirementAssessment.requirement())
                .limit(5)
                .toList();
        List<String> risks = requirementAssessments.stream()
                .filter(requirementAssessment -> requirementAssessment.requirementStatus() == RequirementStatus.MISSING)
                .map(requirementAssessment -> "No matching keywords found for: " + requirementAssessment.requirement())
                .limit(5)
                .toList();
        String recommendation = buildRecommendation(score, evaluationVerdict, risks.size());
        return new JobEvaluationResponse(score, evaluationVerdict, requirementAssessments, strengths, risks, recommendation);
    }

    private RequirementAssessment assessRequirement(String requirement, Set<String> resumeTerms) {
        Set<String> requirementTerms = extractTerms(requirement);
        Set<String> matchedTerms = requirementTerms.stream().filter(resumeTerms::contains).collect(Collectors.toSet());
        double overlap = requirementTerms.isEmpty() ? 0.0 : (double) matchedTerms.size() / requirementTerms.size();
        RequirementStatus requirementStatus;
        if(overlap >= 0.7) {
            requirementStatus = MATCHED;
        } else if(overlap >= 0.35) {
            requirementStatus = PARTIALLY_MATCHED;
        } else {
            requirementStatus = MISSING;
        }
        RequirementType requirementType = isPreferredRequirement(requirement) ? RequirementType.PREFERRED : RequirementType.REQUIRED;
        String evidence = matchedTerms.isEmpty() ? "No matching keywords found" : "Matching keywords: " + String.join(",", matchedTerms);
        String explanation = switch (requirementStatus) {
            case MATCHED -> "Most keywords in this text also appear in the resume.";
            case PARTIALLY_MATCHED -> "Some keywords overlap with the resume.";
            case MISSING -> "Few or no keywords overlap with the resume.";
        };
        return new RequirementAssessment(
                requirement,
                requirementType,
                requirementStatus,
                evidence,
                explanation
        );
    }

    private Set<String> extractTerms(String text) {
        var matcher = WORD_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        Set<String> terms = new LinkedHashSet<>();
        while(matcher.find()) {
            String term = matcher.group();
            if(term.length() > 1 && !STOP_WORDS.contains(term)) {
                terms.add(term);
            }
        }
        return terms;
    }

    private List<String> extractRequirements(String jobDescription) {
        return Arrays.stream(jobDescription.split("(?<=[.!?])\\s+|\\R+"))
                .map(String::trim)
                .filter(sentence -> !sentence.isBlank())
                .filter(sentence -> extractTerms(sentence).size() >= 3)
                .limit(MAX_REQUIREMENTS)
                .toList();
    }

    private boolean isPreferredRequirement(String requirement) {
        String normalized = requirement.toLowerCase(Locale.ROOT);
        return normalized.contains("preferred")
                || normalized.contains("nice to have")
                || normalized.contains("bonus")
                || normalized.contains("optional")
                || normalized.contains("plus");
    }

    private String buildRecommendation(int score, EvaluationVerdict evaluationVerdict, int riskCount) {
        return switch (evaluationVerdict) {
            case APPLY -> "The keyword-based assessment found a strong overlap. "
                    + "Review the requirements and evidence before applying.";
            case APPLY_WITH_RESERVATIONS -> "Some overlap was found, but "
                    + riskCount + " potential gap(s) need review. "
                    + "Check the mandatory requirements before deciding.";
            case LOW_PRIORITY -> "The keyword-based assessment found limited overlap. "
                    + "Verify whether the apparent gaps are genuine before rejecting the role.";
        };
    }

    private int calculateScore(List<RequirementAssessment> requirementAssessments) {
        double totalWeight = 0.0;
        double earnedWeight = 0.0;
        for(RequirementAssessment requirementAssessment : requirementAssessments) {
            double weight = requirementAssessment.requirementType() == RequirementType.REQUIRED ? 2.0 : 1.0;
            totalWeight = totalWeight + weight;
            earnedWeight += switch (requirementAssessment.requirementStatus()) {
                case MATCHED -> weight;
                case PARTIALLY_MATCHED -> weight * 0.5;
                case MISSING -> 0.0;
            };
        }
        return totalWeight == 0 ? 0 : (int) Math.round((earnedWeight/totalWeight) * 100);
    }

    private EvaluationVerdict determineVerdict(int score, List<RequirementAssessment> requirementAssessments) {
        boolean missingRequiredRequirement = requirementAssessments.stream().anyMatch(requirementAssessment -> requirementAssessment.requirementType() == RequirementType.REQUIRED
        && requirementAssessment.requirementStatus() == MISSING);

        if(score > 75 && !missingRequiredRequirement) {
            return EvaluationVerdict.APPLY;
        }
        if (score >= 45) {
            return EvaluationVerdict.APPLY_WITH_RESERVATIONS;
        }

        return EvaluationVerdict.LOW_PRIORITY;
    }
}
