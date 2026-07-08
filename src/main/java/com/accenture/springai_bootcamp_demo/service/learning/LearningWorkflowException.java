package com.accenture.springai_bootcamp_demo.service.learning;

/**
 * Raised when the multi-agent learning workflow cannot complete.
 */
public class LearningWorkflowException extends RuntimeException {

    public LearningWorkflowException(String message, Throwable cause) {
        super(message, cause);
    }
}
