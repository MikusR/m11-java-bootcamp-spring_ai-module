package com.accenture.springai_bootcamp_demo.service.learning;

/**
 * AI boundary for the Learning Path Doctor agents.
 */
public interface LearningAgentClient {

    String complete(String systemPrompt, String userPrompt);
}
