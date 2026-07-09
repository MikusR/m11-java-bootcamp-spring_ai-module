package com.accenture.springai_bootcamp_demo.service.learning;

import java.util.List;
import java.net.URI;
import java.net.Socket;
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

    private static final int CONNECT_TIMEOUT_MILLIS = 500;

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
    public String complete(String systemPrompt, String userPrompt) {
        requireOllamaConfig();
        requireOllamaReachable();
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
            throw new LearningWorkflowException("Ollama is not running at "
                    + ollamaConnectionProperties.getBaseUrl()
                    + ". Start Ollama or choose OpenRouter for the Learning Path provider.", ex);
        }
    }
}
