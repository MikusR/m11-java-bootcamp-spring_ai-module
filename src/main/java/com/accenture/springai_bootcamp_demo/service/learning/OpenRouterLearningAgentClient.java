package com.accenture.springai_bootcamp_demo.service.learning;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiConnectionProperties;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * OpenRouter-backed implementation of the Learning Path Doctor agents.
 */
@Slf4j
@Component
public class OpenRouterLearningAgentClient implements LearningAgentClient {

    private static final String MISSING_API_KEY = "not-configured";
    private static final String SOURCE_BOUNDARY =
            "Use only the learner input and retrieved module guidance. Do not invent course content outside that context. "
                    + "Return the final answer in normal message content only; do not return only reasoning or hidden analysis.";

    private final ChatClient chatClient;
    private final OpenAiChatProperties openAiChatProperties;
    private final OpenAiConnectionProperties openAiConnectionProperties;

    public OpenRouterLearningAgentClient(
            OpenAiChatModel openAiChatModel,
            OpenAiChatProperties openAiChatProperties,
            OpenAiConnectionProperties openAiConnectionProperties
    ) {
        this.chatClient = ChatClient.builder(openAiChatModel).build();
        this.openAiChatProperties = openAiChatProperties;
        this.openAiConnectionProperties = openAiConnectionProperties;
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

                        Rules:
                        - Mention the learner's actual terms when relevant, such as controllers, services, DTOs, CRM, Spring.
                        - Do not mention async, logging, monitoring, data consistency, or testing unless the learner input or retrieved guidance explicitly asks for it.
                        - Keep confidence below 90 unless the learner provided detailed evidence.

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
                        - specific exercise title | number only | concrete instructions

                        Rules:
                        - Do not output the words "title", "duration", or "concrete instructions" as placeholder values.
                        - Each instruction must mention a concrete Spring, controller, service, DTO, validation, test, or repository action.
                        - Produce 3 to 4 steps total.

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
        requireOpenRouterConfig();
        try {
            String content = chatClient.prompt()
                    .messages(List.<Message>of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)))
                    .options(chatOptions())
                    .call()
                    .content();
            if (!StringUtils.hasText(content)) {
                throw new LearningWorkflowException("OpenRouter returned an empty learning workflow response. "
                        + "Use a free non-reasoning OpenRouter model ending with ':free', or set OPENROUTER_MODEL explicitly.", null);
            }
            return content.trim();
        } catch (LearningWorkflowException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("OpenRouter learning agent request failed", ex);
            throw new LearningWorkflowException("Failed to run OpenRouter learning workflow agent: " + ex.getMessage(), ex);
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

    private OpenAiChatOptions chatOptions() {
        return openAiChatProperties.getOptions().copy();
    }

    private void requireOpenRouterConfig() {
        if (!StringUtils.hasText(openAiConnectionProperties.getBaseUrl())) {
            throw new LearningWorkflowException("OpenRouter base URL is not configured", null);
        }
        if (!StringUtils.hasText(openAiConnectionProperties.getApiKey())
                || MISSING_API_KEY.equals(openAiConnectionProperties.getApiKey())) {
            throw new LearningWorkflowException("OpenRouter API key is not configured", null);
        }
        if (openAiChatProperties.getOptions() == null
                || !StringUtils.hasText(openAiChatProperties.getOptions().getModel())) {
            throw new LearningWorkflowException("OpenRouter model is not configured", null);
        }
        if (!openAiChatProperties.getOptions().getModel().endsWith(":free")) {
            throw new LearningWorkflowException("OpenRouter model must be a free model ending with ':free'", null);
        }
    }
}
