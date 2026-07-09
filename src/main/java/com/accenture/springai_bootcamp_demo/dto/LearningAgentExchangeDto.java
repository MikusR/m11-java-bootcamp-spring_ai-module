package com.accenture.springai_bootcamp_demo.dto;

/**
 * Full prompt/response exchange for one Learning Path agent call.
 */
public record LearningAgentExchangeDto(
        String agent,
        String systemPrompt,
        String userPrompt,
        String response) {
}
