package com.accenture.springai_bootcamp_demo.client;

import com.accenture.springai_bootcamp_demo.entity.ChatMessage;
import java.net.URI;
import java.net.Socket;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatProperties;
import org.springframework.ai.model.ollama.autoconfigure.OllamaConnectionProperties;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Thin client over the configured Ollama chat model, backed by Spring AI's
 * {@link ChatClient}. Callers hand over the conversation history and receive
 * the assistant's reply text.
 */
@Slf4j
@Component
public class OllamaClient {
    private static final int CONNECT_TIMEOUT_MILLIS = 500;

    private final ChatClient chatClient;
    private final OllamaChatProperties ollamaChatProperties;
    private final OllamaConnectionProperties ollamaConnectionProperties;

    public OllamaClient(
            ChatClient.Builder chatClientBuilder,
            OllamaChatProperties ollamaChatProperties,
            OllamaConnectionProperties ollamaConnectionProperties
    ) {
        this.chatClient = chatClientBuilder.build();
        this.ollamaChatProperties = ollamaChatProperties;
        this.ollamaConnectionProperties = ollamaConnectionProperties;
    }

    public String complete(List<ChatMessage> history) {
        requireOllamaConfig();
        requireOllamaReachable();
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
        } catch (OllamaException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Ollama request failed", ex);
            throw new OllamaException("Ollama request failed. Confirm Ollama is running at "
                    + ollamaConnectionProperties.getBaseUrl()
                    + " and model "
                    + ollamaChatProperties.getModel()
                    + " is available.", ex);
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

    private OllamaChatOptions chatOptions() {
        return ollamaChatProperties.getOptions().copy();
    }

    private String extractContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new OllamaException("Ollama returned an empty response");
        }
        return content.trim();
    }

    private void requireOllamaConfig() {
        if (!StringUtils.hasText(ollamaConnectionProperties.getBaseUrl())) {
            throw new OllamaException("Ollama base URL is not configured");
        }
        if (!StringUtils.hasText(ollamaChatProperties.getModel())) {
            throw new OllamaException("Ollama chat model is not configured");
        }
    }

    private void requireOllamaReachable() {
        URI uri = URI.create(ollamaConnectionProperties.getBaseUrl());
        int port = uri.getPort();
        if (port == -1) {
            port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(uri.getHost(), port), CONNECT_TIMEOUT_MILLIS);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OllamaException("Ollama is not running at "
                    + ollamaConnectionProperties.getBaseUrl()
                    + ". Start Ollama or create/select an OpenRouter chat.", ex);
        }
    }
}
