package com.accenture.springai_bootcamp_demo.service.learning;

import java.util.List;

/**
 * AI boundary for the Learning Path Doctor agents.
 */
public interface LearningAgentClient {

    String runDiagnostician(
            String learnerGoal,
            String struggles,
            List<LearningKnowledgeBase.RetrievedLearningContext> context);

    String runExerciseDesigner(
            String diagnosis,
            List<LearningKnowledgeBase.RetrievedLearningContext> context,
            int timeAvailableMinutes);

    String runCoach(
            String diagnosis,
            String practicePlan);
}
