package com.accenture.springai_bootcamp_demo.service;

import org.springframework.util.StringUtils;

/**
 * Derives human-friendly chat titles. Kept as a dedicated helper so the
 * orchestration code in {@link ChatService} stays focused on the main flow.
 */
final class ChatTitles {

    static final String PLACEHOLDER = "New chat";
    private static final int MAX_LENGTH = 60;

    private ChatTitles() {
    }

    static String resolveInitial(String requestedTitle) {
        return StringUtils.hasText(requestedTitle) ? requestedTitle.trim() : PLACEHOLDER;
    }

    static boolean isPlaceholder(String title) {
        return title == null || PLACEHOLDER.equals(title.trim());
    }

    /**
     * Builds a concise title from the first user message, collapsing
     * whitespace and truncating to a readable length.
     */
    static String fromFirstMessage(String content) {
        if (!StringUtils.hasText(content)) {
            return PLACEHOLDER;
        }
        String normalized = content.strip().replaceAll("\\s+", " ");
        if (normalized.length() <= MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_LENGTH - 1).trim() + "\u2026";
    }
}
