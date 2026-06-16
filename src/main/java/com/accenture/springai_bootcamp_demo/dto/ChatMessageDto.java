package com.accenture.springai_bootcamp_demo.dto;

import com.accenture.springai_bootcamp_demo.entity.Role;
import java.time.Instant;

/**
 * A single message exposed to API clients.
 */
public record ChatMessageDto(
        Long id,
        Role role,
        String content,
        Instant createdAt) {
}
