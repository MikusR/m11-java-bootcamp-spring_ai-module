package com.accenture.springai_bootcamp_demo.dto;

import java.util.List;

/**
 * RAG-style context snippet selected from the seeded learning knowledge base.
 */
public record RetrievedLearningContextDto(
        String topicId,
        String title,
        List<String> matchedKeywords,
        String guidance) {
}
