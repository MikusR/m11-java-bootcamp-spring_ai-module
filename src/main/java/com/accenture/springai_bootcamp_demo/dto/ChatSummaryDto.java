package com.accenture.springai_bootcamp_demo.dto;

import java.time.Instant;

/**
 * Lightweight chat projection used in list views.
 */
public record ChatSummaryDto(
        String id,
        String title,
        Instant createdAt,
        Instant updatedAt,
        String provider,
        int messageCount) {
}
