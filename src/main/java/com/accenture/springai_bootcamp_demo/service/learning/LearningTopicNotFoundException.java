package com.accenture.springai_bootcamp_demo.service.learning;

/**
 * Raised when a learning path request references an unknown seeded topic.
 */
public class LearningTopicNotFoundException extends RuntimeException {

    public LearningTopicNotFoundException(String topicId) {
        super("Unknown learning topic: " + topicId);
    }
}
