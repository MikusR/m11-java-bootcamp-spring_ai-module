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
    public String complete(String systemPrompt, String userPrompt) {
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
