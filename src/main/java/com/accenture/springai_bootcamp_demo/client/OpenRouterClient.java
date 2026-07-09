package com.accenture.springai_bootcamp_demo.client;

import com.accenture.springai_bootcamp_demo.entity.ChatMessage;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
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
 * Client over the OpenRouter completions API using Spring AI's OpenAiChatModel.
 */
@Slf4j
@Component
public class OpenRouterClient {
    private static final String MISSING_API_KEY = "not-configured";

    private final ChatClient chatClient;
    private final OpenAiChatProperties openAiChatProperties;
    private final OpenAiConnectionProperties openAiConnectionProperties;

    public OpenRouterClient(
            OpenAiChatModel openAiChatModel,
            OpenAiChatProperties openAiChatProperties,
            OpenAiConnectionProperties openAiConnectionProperties
    ) {
        this.chatClient = ChatClient.builder(openAiChatModel).build();
        this.openAiChatProperties = openAiChatProperties;
        this.openAiConnectionProperties = openAiConnectionProperties;
    }

    public String complete(List<ChatMessage> history) {
        requireOpenRouterConfig();
        String reply = call(history);
        return extractContent(reply);
    }

    private String call(List<ChatMessage> history) {
        try {
            return chatClient.prompt()
                    .messages(toSpringAiMessages(history))
                    .options(chatOptions())
                    .call()
                    .content();
        } catch (OpenRouterException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("OpenRouter request failed", ex);
            throw new OpenRouterException("Failed to reach OpenRouter: " + ex.getMessage(), ex);
        }
    }

    private List<Message> toSpringAiMessages(List<ChatMessage> history) {
        return history.stream()
                .map(this::toSpringAiMessage)
                .toList();
    }

    private Message toSpringAiMessage(ChatMessage chatMessage) {
        return switch (chatMessage.getRole()) {
            case SYSTEM -> new SystemMessage(chatMessage.getContent());
            case USER -> new UserMessage(chatMessage.getContent());
            case ASSISTANT -> new AssistantMessage(chatMessage.getContent());
        };
    }

    private OpenAiChatOptions chatOptions() {
        return openAiChatProperties.getOptions().copy();
    }

    private String extractContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new OpenRouterException("OpenRouter returned an empty response");
        }
        return content.trim();
    }

    private void requireOpenRouterConfig() {
        if (!StringUtils.hasText(openAiConnectionProperties.getBaseUrl())) {
            throw new OpenRouterException("OpenRouter base URL is not configured");
        }
        if (!StringUtils.hasText(openAiConnectionProperties.getApiKey())
                || MISSING_API_KEY.equals(openAiConnectionProperties.getApiKey())) {
            throw new OpenRouterException("OpenRouter API key is not configured");
        }
        if (openAiChatProperties.getOptions() == null
                || !StringUtils.hasText(openAiChatProperties.getOptions().getModel())) {
            throw new OpenRouterException("OpenRouter model is not configured");
        }
        if (!openAiChatProperties.getOptions().getModel().endsWith(":free")) {
            throw new OpenRouterException("OpenRouter model must be a free model ending with ':free'");
        }
    }
}
