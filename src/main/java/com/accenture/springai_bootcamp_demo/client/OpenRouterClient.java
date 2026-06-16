package com.accenture.springai_bootcamp_demo.client;

import com.accenture.springai_bootcamp_demo.entity.ChatMessage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Thin client over the OpenRouter chat completions API, backed by Spring AI's
 * {@link ChatClient}. Keeps the public surface intentionally small: callers
 * hand over the conversation history and receive the assistant's reply text.
 */
@Slf4j
@Component
public class OpenRouterClient {
    //TODO add OpenRouter client and config dependency injection


    public String complete(List<ChatMessage> history) {
        requireApiKey();
        String reply = call(history);
        return extractContent(reply);
    }

    private String call(List<ChatMessage> history) {
        try {
            //TODO: add OpenRouter call
            throw new OpenRouterException("Functionality is not yet implemented!");
        } catch (OpenRouterException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("OpenRouter request failed", ex);
            throw new OpenRouterException("Failed to reach OpenRouter: " + ex.getMessage(), ex);
        }
    }

    //TODO: use helper methods for more readable code

    private String extractContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new OpenRouterException("OpenRouter returned an empty response");
        }
        return content.trim();
    }

    private void requireApiKey() {
        //TODO: validate against OpenRouter config
    }
}
