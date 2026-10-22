package com.adarsh.job_application_scoring.evaluation.service;

import com.adarsh.job_application_scoring.evaluation.dto.request.EvaluateJobRequest;
import com.adarsh.job_application_scoring.evaluation.dto.response.JobEvaluationResponse;

public interface JobEvaluationService {

    JobEvaluationResponse evaluate(EvaluateJobRequest evaluateJobRequest);
}
