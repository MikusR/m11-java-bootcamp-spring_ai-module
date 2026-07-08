package com.accenture.springai_bootcamp_demo.client;

/**
 * Raised when the Ollama model cannot be reached or returns an unusable
 * response.
 */
public class OllamaException extends RuntimeException {

    public OllamaException(String message) {
        super(message);
    }

    public OllamaException(String message, Throwable cause) {
        super(message, cause);
    }
}
