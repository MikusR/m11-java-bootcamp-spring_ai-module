package com.accenture.springai_bootcamp_demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openrouter")
public record OpenRouterProperties(){
    //TODO: add OpenRouter config attributes
}
