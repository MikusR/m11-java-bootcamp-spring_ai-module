package com.accenture.springai_bootcamp_demo.service.learning;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatProperties;
import org.springframework.ai.model.ollama.autoconfigure.OllamaConnectionProperties;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Ollama-backed implementation of the three Learning Path Doctor agents.
 */
@Slf4j
@Component
public class OllamaLearningAgentClient implements LearningAgentClient {

    private static final String SOURCE_BOUNDARY =
            "Use only the learner input and retrieved module guidance. Do not invent course content outside that context.";

    private final ChatClient chatClient;
    private final OllamaChatProperties ollamaChatProperties;
    private final OllamaConnectionProperties ollamaConnectionProperties;

    public OllamaLearningAgentClient(
            ChatClient.Builder chatClientBuilder,
            OllamaChatProperties ollamaChatProperties,
            OllamaConnectionProperties ollamaConnectionProperties
    ) {
        this.chatClient = chatClientBuilder.build();
        this.ollamaChatProperties = ollamaChatProperties;
        this.ollamaConnectionProperties = ollamaConnectionProperties;
    }

    @Override
    public String runDiagnostician(
            String learnerGoal,
            String struggles,
            List<LearningKnowledgeBase.RetrievedLearningContext> context
    ) {
        return call(
                "You are a bootcamp learning diagnostician. Identify the learner's likely weak spots using only the learner input and retrieved module guidance. Be specific, practical, and concise. "
                        + SOURCE_BOUNDARY,
                """
                        Return this exact plain-text structure:
                        SUMMARY: one concise sentence
                        WEAK_SPOTS:
                        - weak spot one
                        - weak spot two
                        CONFIDENCE: integer from 0 to 100

                        Learner goal:
                        %s

                        Current struggles:
                        %s

                        Retrieved module guidance:
                        %s
                        """.formatted(learnerGoal, struggles, formatContext(context)));
    }

    @Override
    public String runExerciseDesigner(
            String diagnosis,
            List<LearningKnowledgeBase.RetrievedLearningContext> context,
            int timeAvailableMinutes
    ) {
        return call(
                "You are a bootcamp exercise designer. Create practice steps that fit the available time. Each step must have a title, duration in minutes, and concrete instructions. "
                        + SOURCE_BOUNDARY,
                """
                        Return only practice steps in this exact format, one per line:
                        - title | durationMinutes | concrete instructions

                        Available time: %d minutes

                        Diagnosis:
                        %s

                        Retrieved module guidance:
                        %s
                        """.formatted(timeAvailableMinutes, diagnosis, formatContext(context)));
    }

    @Override
    public String runCoach(String diagnosis, String practicePlan) {
        return call(
                "You are a direct but supportive learning coach. Summarize the diagnosis and practice plan in plain language. Avoid vague encouragement. "
                        + SOURCE_BOUNDARY,
                """
                        Write one short paragraph for the learner.

                        Diagnosis:
                        %s

                        Practice plan:
                        %s
                        """.formatted(diagnosis, practicePlan));
    }

    private String call(String systemPrompt, String userPrompt) {
        requireOllamaConfig();
        try {
            String content = chatClient.prompt()
                    .messages(List.<Message>of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)))
                    .options(chatOptions())
                    .call()
                    .content();
            if (!StringUtils.hasText(content)) {
                throw new LearningWorkflowException("Ollama returned an empty learning workflow response", null);
            }
            return content.trim();
        } catch (LearningWorkflowException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Learning agent request failed", ex);
            throw new LearningWorkflowException("Failed to run learning workflow agent: " + ex.getMessage(), ex);
        }
    }

    private String formatContext(List<LearningKnowledgeBase.RetrievedLearningContext> context) {
        StringBuilder builder = new StringBuilder();
        for (LearningKnowledgeBase.RetrievedLearningContext item : context) {
            builder.append("- ")
                    .append(item.title())
                    .append(": ")
                    .append(item.guidance())
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    private OllamaChatOptions chatOptions() {
        return ollamaChatProperties.getOptions().copy();
    }

    private void requireOllamaConfig() {
        if (!StringUtils.hasText(ollamaConnectionProperties.getBaseUrl())) {
            throw new LearningWorkflowException("Ollama base URL is not configured", null);
        }
        if (!StringUtils.hasText(ollamaChatProperties.getModel())) {
            throw new LearningWorkflowException("Ollama chat model is not configured", null);
        }
    }
}
