package com.accenture.springai_bootcamp_demo.dto;

import java.util.List;

/**
 * Full response from the Learning Path Doctor workflow.
 */
public record LearningDiagnosisResponse(
        LearningDiagnosisDto diagnosis,
        List<RetrievedLearningContextDto> retrievedContext,
        PracticePlanDto practicePlan,
        String coachMessage,
        List<AgentTraceDto> agentTrace) {
}
