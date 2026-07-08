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
                .contains("java-core", "oop", "spring-crm", "spring-boot", "spring-ai", "testing");
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

    @Test
    void diagnoseReplacesGenericModelOutputAndPlaceholderExercises() {
        LearningPathService crmService = new LearningPathService(knowledgeBase, new GenericLearningAgentClient());
        LearningDiagnosisRequest request = new LearningDiagnosisRequest(
                "I want to build a Spring CRM.",
                "I understand controllers but get confused about services, DTOs.",
                List.of("spring-boot", "java-core", "validation"),
                45);

        LearningDiagnosisResponse response = crmService.diagnose(request);

        assertThat(response.diagnosis().summary()).contains("Spring CRM");
        assertThat(response.diagnosis().weakSpots())
                .contains(
                        "Connecting controller endpoints to focused service methods without putting business logic in the controller",
                        "Designing request and response DTOs that keep the API separate from internal entities");
        assertThat(response.diagnosis().weakSpots())
                .noneMatch(weakSpot -> weakSpot.toLowerCase().contains("logging"));
        assertThat(response.practicePlan().steps())
                .extracting("title")
                .containsExactly("Sketch the CRM API flow", "Design DTOs for one request", "Move behavior into the service");
        assertThat(response.practicePlan().steps())
                .extracting("instructions")
                .noneMatch(instructions -> ((String) instructions).contains("no concrete instructions"));
    }

    @Test
    void diagnoseReplacesSmallModelAcknowledgementResponse() {
        LearningPathService crmService = new LearningPathService(knowledgeBase, new AcknowledgementLearningAgentClient());
        LearningDiagnosisRequest request = new LearningDiagnosisRequest(
                "I want to build a Spring CRM.",
                "I understand controllers but get confused about services, DTOs.",
                List.of("spring-crm", "spring-boot", "java-core"),
                45);

        LearningDiagnosisResponse response = crmService.diagnose(request);

        assertThat(response.diagnosis().summary()).isEqualTo(
                "Focus on translating the Spring CRM idea into controller endpoints, service use cases, and DTO-based API contracts.");
        assertThat(response.diagnosis().weakSpots())
                .contains("Breaking the CRM feature into small use cases such as create customer, update customer, and list interactions");
        assertThat(response.practicePlan().steps())
                .extracting("title")
                .containsExactly("Sketch the CRM API flow", "Design DTOs for one request", "Move behavior into the service");
        assertThat(response.coachMessage()).contains("controller, DTO, service, and repository");
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

    private static class GenericLearningAgentClient implements LearningAgentClient {

        @Override
        public String runDiagnostician(
                String learnerGoal,
                String struggles,
                List<LearningKnowledgeBase.RetrievedLearningContext> context
        ) {
            return """
                    SUMMARY: A Spring MVC application needs to handle asynchronous operations, manage request/response lifecycle, and ensure data consistency.
                    WEAK_SPOTS:
                    - Lack of proper API design and error handling.
                    - Inadequate logging and monitoring.
                    CONFIDENCE: 100
                    """;
        }

        @Override
        public String runExerciseDesigner(
                String diagnosis,
                List<LearningKnowledgeBase.RetrievedLearningContext> context,
                int timeAvailableMinutes
        ) {
            return """
                    - title | 10 | concrete instructions
                    - duration | 15 | no concrete instructions
                    """;
        }

        @Override
        public String runCoach(String diagnosis, String practicePlan) {
            return "Use the generated practice plan.";
        }
    }

    private static class AcknowledgementLearningAgentClient implements LearningAgentClient {

        @Override
        public String runDiagnostician(
                String learnerGoal,
                String struggles,
                List<LearningKnowledgeBase.RetrievedLearningContext> context
        ) {
            return "Okay, I understand. I will analyze your learner input and retrieved module guidance based on the criteria outlined above. Let's begin!";
        }

        @Override
        public String runExerciseDesigner(
                String diagnosis,
                List<LearningKnowledgeBase.RetrievedLearningContext> context,
                int timeAvailableMinutes
        ) {
            return "- title | 10 | concrete instructions";
        }

        @Override
        public String runCoach(String diagnosis, String practicePlan) {
            return "Okay, let's start! I understand the diagnostic process.";
        }
    }
}
