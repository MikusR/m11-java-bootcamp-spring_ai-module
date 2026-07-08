package com.accenture.springai_bootcamp_demo.client;

import com.accenture.springai_bootcamp_demo.config.OpenRouterProperties;
import com.accenture.springai_bootcamp_demo.entity.ChatMessage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

/**
 * Thin client over the OpenRouter chat completions API. Keeps the public surface
 * intentionally small: callers
 * hand over the conversation history and receive the assistant's reply text.
 */
@Slf4j
@Component
public class OpenRouterClient {
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final RestClient restClient;
    private final OpenRouterProperties openRouterProperties;

    public OpenRouterClient(RestClient.Builder restClientBuilder, OpenRouterProperties openRouterProperties) {
        this.openRouterProperties = openRouterProperties;
        this.restClient = restClientBuilder
                .baseUrl(openRouterProperties.baseUrl())
                .build();
    }

    public String complete(List<ChatMessage> history) {
        requireOpenRouterConfig();
        String reply = call(history);
        return extractContent(reply);
    }

    private String call(List<ChatMessage> history) {
        try {
            ChatCompletionResponse response = restClient.post()
                    .uri(CHAT_COMPLETIONS_PATH)
                    .headers(headers -> headers.setBearerAuth(openRouterProperties.apiKey()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(chatCompletionRequest(history))
                    .retrieve()
                    .body(ChatCompletionResponse.class);
            return firstChoiceContent(response);
        } catch (OpenRouterException ex) {
            throw ex;
        } catch (HttpStatusCodeException ex) {
            log.error("OpenRouter request failed with status {}", ex.getStatusCode(), ex);
            throw new OpenRouterException("OpenRouter returned " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString(), ex);
        } catch (RuntimeException ex) {
            log.error("OpenRouter request failed", ex);
            throw new OpenRouterException("Failed to reach OpenRouter: " + ex.getMessage(), ex);
        }
    }

    private Map<String, Object> chatCompletionRequest(List<ChatMessage> history) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", openRouterProperties.model());
        request.put("messages", toOpenRouterMessages(history));

        if (openRouterProperties.temperature() != null) {
            request.put("temperature", openRouterProperties.temperature());
        }
        if (openRouterProperties.maxCompletionTokens() != null) {
            request.put("max_completion_tokens", openRouterProperties.maxCompletionTokens());
        }

        return request;
    }

    private List<Map<String, String>> toOpenRouterMessages(List<ChatMessage> history) {
        return history.stream()
                .map(this::toOpenRouterMessage)
                .toList();
    }

    private Map<String, String> toOpenRouterMessage(ChatMessage chatMessage) {
        Map<String, String> message = new LinkedHashMap<>();
        message.put("role", chatMessage.getRole().name().toLowerCase());
        message.put("content", chatMessage.getContent());
        return message;
    }

    private String firstChoiceContent(ChatCompletionResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new OpenRouterException("OpenRouter returned no choices");
        }

        Choice firstChoice = response.choices().getFirst();
        if (firstChoice.message() == null) {
            throw new OpenRouterException("OpenRouter returned a choice without a message");
        }

        return firstChoice.message().content();
    }

    private String extractContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new OpenRouterException("OpenRouter returned an empty response");
        }
        return content.trim();
    }

    private void requireOpenRouterConfig() {
        if (!StringUtils.hasText(openRouterProperties.baseUrl())) {
            throw new OpenRouterException("OpenRouter base URL is not configured");
        }
        if (!StringUtils.hasText(openRouterProperties.apiKey())) {
            throw new OpenRouterException("OpenRouter API key is not configured");
        }
        if (!StringUtils.hasText(openRouterProperties.model())) {
            throw new OpenRouterException("OpenRouter model is not configured");
        }
    }

    private record ChatCompletionResponse(List<Choice> choices) {
    }

    private record Choice(ResponseMessage message) {
    }

    private record ResponseMessage(String content) {
    }
}
