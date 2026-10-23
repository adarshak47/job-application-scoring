package com.adarsh.job_application_scoring.ai.client;

import com.adarsh.job_application_scoring.ai.dto.GeminiAssessmentPayload;
import com.adarsh.job_application_scoring.evaluation.dto.request.EvaluateJobRequest;

public interface AiEvaluationClient {

    GeminiAssessmentPayload assess(EvaluateJobRequest evaluateJobRequest);
}
