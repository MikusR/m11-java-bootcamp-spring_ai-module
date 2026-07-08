package com.accenture.springai_bootcamp_demo.service.learning;

import static org.assertj.core.api.Assertions.assertThat;

import com.accenture.springai_bootcamp_demo.dto.LearningDiagnosisRequest;
import com.accenture.springai_bootcamp_demo.dto.LearningDiagnosisResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LearningPathServiceTest {

    private final LearningKnowledgeBase knowledgeBase = new LearningKnowledgeBase(new LearningRetriever());
    private final FakeLearningAgentClient agentClient = new FakeLearningAgentClient();
    private final LearningPathService service = new LearningPathService(knowledgeBase, agentClient);

    @Test
    void listTopicsMapsSeededTopics() {
        assertThat(service.listTopics())
                .extracting("id")
                .contains("java-core", "oop", "spring-boot", "spring-ai", "testing");
        assertThat(service.listTopics())
                .extracting("article")
                .allSatisfy(article -> assertThat((String) article).isNotBlank());
    }

    @Test
    void diagnoseCallsAgentsInOrderAndReturnsStructuredResponse() {
        LearningDiagnosisRequest request = new LearningDiagnosisRequest(
                "I want to build a Spring AI chatbot confidently.",
                "I understand controllers but get confused about services, DTOs, and AI prompts.",
                List.of("java-core", "spring-boot", "spring-ai"),
                45);

        LearningDiagnosisResponse response = service.diagnose(request);

        assertThat(agentClient.calls).containsExactly("diagnostician", "exerciseDesigner", "coach");
        assertThat(response.retrievedContext()).hasSize(3);
        assertThat(response.diagnosis().summary()).isEqualTo("The learner needs to connect API structure with AI orchestration.");
        assertThat(response.diagnosis().weakSpots()).contains("Mapping controller requests into service workflows");
        assertThat(response.diagnosis().confidenceScore()).isEqualTo(68);
        assertThat(response.practicePlan().timeBoxMinutes()).isEqualTo(45);
        assertThat(response.practicePlan().steps()).hasSize(2);
        assertThat(response.coachMessage()).contains("Trace one request");
        assertThat(response.agentTrace())
                .extracting("agent")
                .containsExactly("DIAGNOSTICIAN", "EXERCISE_DESIGNER", "COACH");
    }

    private static class FakeLearningAgentClient implements LearningAgentClient {

        private final List<String> calls = new ArrayList<>();

        @Override
        public String runDiagnostician(
                String learnerGoal,
                String struggles,
                List<LearningKnowledgeBase.RetrievedLearningContext> context
        ) {
            calls.add("diagnostician");
            return """
                    SUMMARY: The learner needs to connect API structure with AI orchestration.
                    WEAK_SPOTS:
                    - Mapping controller requests into service workflows
                    - Understanding prompt context and fake model tests
                    CONFIDENCE: 68
                    """;
        }

        @Override
        public String runExerciseDesigner(
                String diagnosis,
                List<LearningKnowledgeBase.RetrievedLearningContext> context,
                int timeAvailableMinutes
        ) {
            calls.add("exerciseDesigner");
            return """
                    - Trace the request flow | 15 | Follow a POST request from controller to service to agent client.
                    - Write a fake-agent test | 30 | Replace the AI boundary and assert the orchestration order.
                    """;
        }

        @Override
        public String runCoach(String diagnosis, String practicePlan) {
            calls.add("coach");
            return "Trace one request, then test the workflow with a fake model client.";
        }
    }
}
