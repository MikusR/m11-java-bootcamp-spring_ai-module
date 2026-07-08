package com.accenture.springai_bootcamp_demo.service.learning;

import com.accenture.springai_bootcamp_demo.dto.AgentTraceDto;
import com.accenture.springai_bootcamp_demo.dto.LearningDiagnosisDto;
import com.accenture.springai_bootcamp_demo.dto.LearningDiagnosisRequest;
import com.accenture.springai_bootcamp_demo.dto.LearningDiagnosisResponse;
import com.accenture.springai_bootcamp_demo.dto.LearningTopicDto;
import com.accenture.springai_bootcamp_demo.dto.PracticePlanDto;
import com.accenture.springai_bootcamp_demo.dto.PracticeStepDto;
import com.accenture.springai_bootcamp_demo.dto.RetrievedLearningContextDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the Learning Path Doctor retrieval and multi-agent workflow.
 */
@Service
public class LearningPathService {

    private static final Pattern CONFIDENCE_PATTERN = Pattern.compile("(?i)confidence\\s*:?\\s*(\\d{1,3})");
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d{1,3})");

    private final LearningKnowledgeBase learningKnowledgeBase;
    private final LearningAgentClient learningAgentClient;

    public LearningPathService(
            LearningKnowledgeBase learningKnowledgeBase,
            LearningAgentClient learningAgentClient
    ) {
        this.learningKnowledgeBase = learningKnowledgeBase;
        this.learningAgentClient = learningAgentClient;
    }

    public List<LearningTopicDto> listTopics() {
        return learningKnowledgeBase.allTopics().stream()
                .map(topic -> new LearningTopicDto(topic.id(), topic.title(), topic.summary(), topic.article()))
                .toList();
    }

    public LearningDiagnosisResponse diagnose(LearningDiagnosisRequest request) {
        List<LearningKnowledgeBase.LearningTopic> selectedTopics = learningKnowledgeBase.findTopics(request.topics());
        String retrievalQuery = buildRetrievalQuery(request, selectedTopics);
        List<LearningKnowledgeBase.RetrievedLearningContext> context =
                learningKnowledgeBase.retrieve(request.topics(), retrievalQuery);

        String diagnosisText = learningAgentClient.runDiagnostician(
                request.learnerGoal(),
                request.struggles(),
                context);
        LearningDiagnosisDto diagnosis = parseDiagnosis(diagnosisText);

        String exerciseText = learningAgentClient.runExerciseDesigner(
                diagnosisText,
                context,
                request.timeAvailableMinutes());
        PracticePlanDto practicePlan = parsePracticePlan(exerciseText, request.timeAvailableMinutes());

        String coachMessage = learningAgentClient.runCoach(diagnosisText, formatPracticePlan(practicePlan));

        return new LearningDiagnosisResponse(
                diagnosis,
                toContextDtos(context),
                practicePlan,
                coachMessage,
                agentTrace());
    }

    private String buildRetrievalQuery(
            LearningDiagnosisRequest request,
            List<LearningKnowledgeBase.LearningTopic> selectedTopics
    ) {
        String topicTitles = selectedTopics.stream()
                .map(LearningKnowledgeBase.LearningTopic::title)
                .reduce("", (left, right) -> left + " " + right);
        return String.join(" ", request.learnerGoal(), request.struggles(), topicTitles);
    }

    private LearningDiagnosisDto parseDiagnosis(String diagnosisText) {
        String summary = extractSummary(diagnosisText);
        List<String> weakSpots = extractWeakSpots(diagnosisText);
        int confidenceScore = extractConfidenceScore(diagnosisText);
        return new LearningDiagnosisDto(summary, weakSpots, confidenceScore);
    }

    private String extractSummary(String diagnosisText) {
        for (String line : diagnosisText.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("summary:")) {
                String summary = trimmed.substring(trimmed.indexOf(':') + 1).trim();
                if (!summary.isBlank()) {
                    return summary;
                }
            }
        }
        return diagnosisText.trim();
    }

    private List<String> extractWeakSpots(String diagnosisText) {
        List<String> weakSpots = new ArrayList<>();
        boolean inWeakSpots = false;
        for (String line : diagnosisText.split("\\R")) {
            String trimmed = line.trim();
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (lower.startsWith("weak_spots") || lower.startsWith("weak spots")) {
                inWeakSpots = true;
                continue;
            }
            if (inWeakSpots && lower.startsWith("confidence")) {
                break;
            }
            if (inWeakSpots && (trimmed.startsWith("-") || trimmed.startsWith("*"))) {
                String weakSpot = trimmed.substring(1).trim();
                if (!weakSpot.isBlank()) {
                    weakSpots.add(weakSpot);
                }
            }
        }
        if (weakSpots.isEmpty()) {
            weakSpots.add(diagnosisText.trim());
        }
        return weakSpots;
    }

    private int extractConfidenceScore(String diagnosisText) {
        Matcher matcher = CONFIDENCE_PATTERN.matcher(diagnosisText);
        if (!matcher.find()) {
            return 50;
        }
        int parsed = Integer.parseInt(matcher.group(1));
        return Math.max(0, Math.min(100, parsed));
    }

    private PracticePlanDto parsePracticePlan(String exerciseText, int timeAvailableMinutes) {
        List<PracticeStepDto> steps = new ArrayList<>();
        for (String line : exerciseText.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            PracticeStepDto step = parsePracticeStep(trimmed);
            if (step != null) {
                steps.add(step);
            }
        }
        if (steps.isEmpty()) {
            steps.add(new PracticeStepDto("Review generated guidance", timeAvailableMinutes, exerciseText.trim()));
        }
        return new PracticePlanDto(timeAvailableMinutes, steps);
    }

    private PracticeStepDto parsePracticeStep(String line) {
        String normalized = line.replaceFirst("^[-*]\\s*", "");
        String[] parts = normalized.split("\\|", 3);
        if (parts.length == 3) {
            int duration = parseDuration(parts[1], 10);
            return new PracticeStepDto(parts[0].trim(), duration, parts[2].trim());
        }
        return null;
    }

    private int parseDuration(String value, int defaultDuration) {
        Matcher matcher = DURATION_PATTERN.matcher(value);
        if (!matcher.find()) {
            return defaultDuration;
        }
        return Math.max(1, Integer.parseInt(matcher.group(1)));
    }

    private String formatPracticePlan(PracticePlanDto practicePlan) {
        StringBuilder builder = new StringBuilder();
        builder.append("Time box: ").append(practicePlan.timeBoxMinutes()).append(" minutes").append(System.lineSeparator());
        for (PracticeStepDto step : practicePlan.steps()) {
            builder.append("- ")
                    .append(step.title())
                    .append(" (")
                    .append(step.durationMinutes())
                    .append(" minutes): ")
                    .append(step.instructions())
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    private List<RetrievedLearningContextDto> toContextDtos(
            List<LearningKnowledgeBase.RetrievedLearningContext> context
    ) {
        return context.stream()
                .map(item -> new RetrievedLearningContextDto(
                        item.topicId(),
                        item.title(),
                        item.matchedKeywords(),
                        item.guidance()))
                .toList();
    }

    private List<AgentTraceDto> agentTrace() {
        return List.of(
                new AgentTraceDto("DIAGNOSTICIAN", "Identify weak spots from learner input and retrieved context."),
                new AgentTraceDto("EXERCISE_DESIGNER", "Create targeted exercises from diagnosis and time budget."),
                new AgentTraceDto("COACH", "Turn the plan into concise learner-facing advice.")
        );
    }
}
