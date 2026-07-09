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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the Learning Path Doctor retrieval and multi-agent workflow.
 */
@Service
public class LearningPathService {

    private static final Pattern CONFIDENCE_PATTERN = Pattern.compile("(?i)confidence\\s*:?\\s*(\\d{1,3})");
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d{1,3})");

    private final LearningKnowledgeBase learningKnowledgeBase;
    private final LearningAgentClient ollamaLearningAgentClient;
    private final LearningAgentClient openRouterLearningAgentClient;

    @Autowired
    public LearningPathService(
            LearningKnowledgeBase learningKnowledgeBase,
            @Qualifier("ollamaLearningAgentClient") LearningAgentClient ollamaLearningAgentClient,
            @Qualifier("openRouterLearningAgentClient") LearningAgentClient openRouterLearningAgentClient
    ) {
        this.learningKnowledgeBase = learningKnowledgeBase;
        this.ollamaLearningAgentClient = ollamaLearningAgentClient;
        this.openRouterLearningAgentClient = openRouterLearningAgentClient;
    }

    LearningPathService(
            LearningKnowledgeBase learningKnowledgeBase,
            LearningAgentClient learningAgentClient
    ) {
        this.learningKnowledgeBase = learningKnowledgeBase;
        this.ollamaLearningAgentClient = learningAgentClient;
        this.openRouterLearningAgentClient = learningAgentClient;
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
        LearningAgentClient learningAgentClient = learningAgentClient(request.provider());

        String diagnosisText = learningAgentClient.runDiagnostician(
                request.learnerGoal(),
                request.struggles(),
                context);
        LearningDiagnosisDto diagnosis = parseDiagnosis(diagnosisText, request, context);

        String exerciseText = learningAgentClient.runExerciseDesigner(
                formatDiagnosis(diagnosis),
                context,
                request.timeAvailableMinutes());
        PracticePlanDto practicePlan = parsePracticePlan(exerciseText, request, diagnosis, context);

        String coachMessage = learningAgentClient.runCoach(formatDiagnosis(diagnosis), formatPracticePlan(practicePlan));
        coachMessage = sanitizeCoachMessage(coachMessage, diagnosis, practicePlan);

        return new LearningDiagnosisResponse(
                diagnosis,
                toContextDtos(context),
                practicePlan,
                coachMessage,
                agentTrace());
    }

    private LearningAgentClient learningAgentClient(String provider) {
        return "openrouter".equalsIgnoreCase(provider)
                ? openRouterLearningAgentClient
                : ollamaLearningAgentClient;
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

    private LearningDiagnosisDto parseDiagnosis(
            String diagnosisText,
            LearningDiagnosisRequest request,
            List<LearningKnowledgeBase.RetrievedLearningContext> context
    ) {
        String summary = extractSummary(diagnosisText);
        List<String> weakSpots = extractWeakSpots(diagnosisText);
        int confidenceScore = extractConfidenceScore(diagnosisText);
        if (isGenericDiagnosis(summary, weakSpots, confidenceScore, request, context)) {
            return fallbackDiagnosis(request);
        }
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

    private PracticePlanDto parsePracticePlan(
            String exerciseText,
            LearningDiagnosisRequest request,
            LearningDiagnosisDto diagnosis,
            List<LearningKnowledgeBase.RetrievedLearningContext> context
    ) {
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
            steps.addAll(fallbackPracticeSteps(request, diagnosis, context));
        }
        return new PracticePlanDto(request.timeAvailableMinutes(), steps);
    }

    private PracticeStepDto parsePracticeStep(String line) {
        String normalized = line.replaceFirst("^[-*]\\s*", "");
        String[] parts = normalized.split("\\|", 3);
        if (parts.length == 3) {
            if (isPlaceholderStep(parts)) {
                return null;
            }
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

    private boolean isGenericDiagnosis(
            String summary,
            List<String> weakSpots,
            int confidenceScore,
            LearningDiagnosisRequest request,
            List<LearningKnowledgeBase.RetrievedLearningContext> context
    ) {
        String modelText = String.join(" ", summary, String.join(" ", weakSpots)).toLowerCase(Locale.ROOT);
        String allowedText = String.join(" ", request.learnerGoal(), request.struggles(), formatContextText(context))
                .toLowerCase(Locale.ROOT);
        String learnerText = String.join(" ", request.learnerGoal(), request.struggles()).toLowerCase(Locale.ROOT);
        List<String> unsupportedSignals = List.of(
                "asynchronous",
                "cmm",
                "data consistency",
                "logging",
                "monitoring",
                "request/response lifecycle"
        );
        boolean containsUnsupportedSignal = unsupportedSignals.stream()
                .anyMatch(signal -> modelText.contains(signal) && !allowedText.contains(signal));
        boolean missesLearnerLanguage = importantLearnerTerms(request).stream()
                .noneMatch(modelText::contains);
        boolean overconfidentTinyModelResponse = confidenceScore > 90;
        boolean crmPromptWithoutCrmDiagnosis = learnerText.contains("crm") && !modelText.contains("crm");
        return containsUnsupportedSignal
                || missesLearnerLanguage
                || overconfidentTinyModelResponse
                || crmPromptWithoutCrmDiagnosis
                || summary.length() > 300
                || isMetaResponse(modelText)
                || isSingleEchoedWeakSpot(summary, weakSpots);
    }

    private LearningDiagnosisDto fallbackDiagnosis(LearningDiagnosisRequest request) {
        Set<String> weakSpots = new LinkedHashSet<>();
        String learnerText = String.join(" ", request.learnerGoal(), request.struggles()).toLowerCase(Locale.ROOT);

        if (learnerText.contains("controller") || learnerText.contains("service")) {
            weakSpots.add("Connecting controller endpoints to focused service methods without putting business logic in the controller");
        }
        if (learnerText.contains("dto")) {
            weakSpots.add("Designing request and response DTOs that keep the API separate from internal entities");
        }
        if (learnerText.contains("crm")) {
            weakSpots.add("Breaking the CRM feature into small use cases such as create customer, update customer, and list interactions");
        }
        if (weakSpots.isEmpty()) {
            weakSpots.add("Turning the learner goal into small Spring Boot implementation steps");
        }

        return new LearningDiagnosisDto(
                "Focus on translating the Spring CRM idea into controller endpoints, service use cases, and DTO-based API contracts.",
                List.copyOf(weakSpots),
                75);
    }

    private List<PracticeStepDto> fallbackPracticeSteps(
            LearningDiagnosisRequest request,
            LearningDiagnosisDto diagnosis,
            List<LearningKnowledgeBase.RetrievedLearningContext> context
    ) {
        int total = request.timeAvailableMinutes();
        int first = Math.max(10, total / 3);
        int second = Math.max(10, total / 3);
        int third = Math.max(10, total - first - second);
        String contextTitles = context.stream()
                .map(LearningKnowledgeBase.RetrievedLearningContext::title)
                .reduce((left, right) -> left + ", " + right)
                .orElse("the selected topics");

        return List.of(
                new PracticeStepDto(
                        "Sketch the CRM API flow",
                        first,
                        "Write the endpoints for one CRM use case, then mark which class owns each step: controller, service, DTO, and repository."),
                new PracticeStepDto(
                        "Design DTOs for one request",
                        second,
                        "Create a request DTO and response DTO for the CRM use case. Add validation rules for required fields and size limits."),
                new PracticeStepDto(
                        "Move behavior into the service",
                        third,
                        "Implement the service method using the guidance from " + contextTitles
                                + ". Keep controller code limited to validation, delegation, and returning the response DTO. Main diagnosis: "
                                + diagnosis.summary())
        );
    }

    private String sanitizeCoachMessage(
            String coachMessage,
            LearningDiagnosisDto diagnosis,
            PracticePlanDto practicePlan
    ) {
        String lower = coachMessage.toLowerCase(Locale.ROOT);
        if (!isMetaResponse(lower) && !lower.contains("diagnostic process") && !lower.contains("cmm")) {
            return coachMessage;
        }
        String firstStep = practicePlan.steps().isEmpty() ? "start with one vertical slice" : practicePlan.steps().getFirst().title();
        return "Focus on this concrete gap: " + removeLeadingFocus(diagnosis.summary())
                + " Start with \"" + firstStep
                + "\", then keep each CRM use case moving through controller, DTO, service, and repository one step at a time.";
    }

    private String removeLeadingFocus(String value) {
        return value.replaceFirst("(?i)^focus on\\s+", "");
    }

    private boolean isMetaResponse(String lowerText) {
        return lowerText.startsWith("okay, i understand")
                || lowerText.startsWith("okay, let's")
                || lowerText.startsWith("okay, here's")
                || lowerText.contains("i will analyze")
                || lowerText.contains("let's begin")
                || lowerText.contains("criteria outlined above");
    }

    private String formatDiagnosis(LearningDiagnosisDto diagnosis) {
        StringBuilder builder = new StringBuilder();
        builder.append("SUMMARY: ").append(diagnosis.summary()).append(System.lineSeparator());
        builder.append("WEAK_SPOTS:").append(System.lineSeparator());
        for (String weakSpot : diagnosis.weakSpots()) {
            builder.append("- ").append(weakSpot).append(System.lineSeparator());
        }
        builder.append("CONFIDENCE: ").append(diagnosis.confidenceScore());
        return builder.toString();
    }

    private boolean isSingleEchoedWeakSpot(String summary, List<String> weakSpots) {
        if (weakSpots.size() != 1) {
            return false;
        }
        String weakSpot = weakSpots.getFirst();
        return weakSpot.equals(summary) || weakSpot.equals("SUMMARY: " + summary);
    }

    private boolean isPlaceholderStep(String[] parts) {
        String title = parts[0].trim().toLowerCase(Locale.ROOT);
        String duration = parts[1].trim().toLowerCase(Locale.ROOT);
        String instructions = parts[2].trim().toLowerCase(Locale.ROOT);
        return title.equals("title")
                || title.equals("duration")
                || duration.equals("duration")
                || instructions.equals("concrete instructions")
                || instructions.equals("no concrete instructions")
                || instructions.isBlank();
    }

    private List<String> importantLearnerTerms(LearningDiagnosisRequest request) {
        String learnerText = String.join(" ", request.learnerGoal(), request.struggles()).toLowerCase(Locale.ROOT);
        List<String> candidates = List.of("controller", "service", "dto", "crm", "spring");
        return candidates.stream()
                .filter(learnerText::contains)
                .toList();
    }

    private String formatContextText(List<LearningKnowledgeBase.RetrievedLearningContext> context) {
        return context.stream()
                .map(item -> item.title() + " " + item.guidance())
                .reduce("", (left, right) -> left + " " + right);
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
