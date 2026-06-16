package com.accenture.springai_bootcamp_demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for submitting a user message to a chat.
 */
public record SendMessageRequest(
        @NotBlank(message = "content must not be blank")
        @Size(max = 8000, message = "content must not exceed 8000 characters")
        String content) {
}
