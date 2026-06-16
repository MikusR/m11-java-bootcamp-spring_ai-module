package com.accenture.springai_bootcamp_demo.service;

/**
 * Raised when a referenced chat does not exist. Mapped to HTTP 404 by the
 * global exception handler.
 */
public class ChatNotFoundException extends RuntimeException {

    public ChatNotFoundException(String chatId) {
        super("Chat not found: " + chatId);
    }
}
