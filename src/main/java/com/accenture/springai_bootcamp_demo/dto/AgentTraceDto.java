package com.accenture.springai_bootcamp_demo.dto;

/**
 * Lightweight trace entry that explains each agent's role in the workflow.
 */
public record AgentTraceDto(
        String agent,
        String purpose) {
}
