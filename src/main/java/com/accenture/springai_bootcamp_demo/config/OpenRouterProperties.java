package com.accenture.springai_bootcamp_demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.ai.openrouter")
public record OpenRouterProperties(
        String baseUrl,
        String apiKey,
        String model,
        Double temperature,
        Integer maxCompletionTokens
) {
    private static final String DEFAULT_BASE_URL = "https://openrouter.ai/api/v1";

    public OpenRouterProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = DEFAULT_BASE_URL;
        }
    }
}
