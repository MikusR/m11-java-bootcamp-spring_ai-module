package com.accenture.springai_bootcamp_demo.dto;

import jakarta.validation.constraints.Size;

/**
 * Request body for creating a new chat. Title is optional and defaults
 * to a generated placeholder when omitted.
 */
public record CreateChatRequest(
        @Size(max = 200, message = "title must not exceed 200 characters")
        String title) {
}
