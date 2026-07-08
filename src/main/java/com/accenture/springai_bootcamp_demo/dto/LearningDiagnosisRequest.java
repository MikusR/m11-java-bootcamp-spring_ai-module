package com.accenture.springai_bootcamp_demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request body for diagnosing a learner's current bootcamp learning path.
 */
public record LearningDiagnosisRequest(
        @NotBlank(message = "learnerGoal must not be blank")
        @Size(max = 1000, message = "learnerGoal must not exceed 1000 characters")
        String learnerGoal,

        @NotBlank(message = "struggles must not be blank")
        @Size(max = 3000, message = "struggles must not exceed 3000 characters")
        String struggles,

        @NotEmpty(message = "topics must contain at least one topic")
        @Size(max = 5, message = "topics must not contain more than 5 items")
        List<@NotBlank(message = "topic id must not be blank") String> topics,

        @NotNull(message = "timeAvailableMinutes is required")
        @Min(value = 15, message = "timeAvailableMinutes must be at least 15")
        @Max(value = 240, message = "timeAvailableMinutes must not exceed 240")
        Integer timeAvailableMinutes) {
}
