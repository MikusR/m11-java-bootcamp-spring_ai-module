package com.accenture.springai_bootcamp_demo.dto;

/**
 * Public summary of a learning topic available to the diagnostic workflow.
 */
public record LearningTopicDto(
        String id,
        String title,
        String summary,
        String article) {
}
