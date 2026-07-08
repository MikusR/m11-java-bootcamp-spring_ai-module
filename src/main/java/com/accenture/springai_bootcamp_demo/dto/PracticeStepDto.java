package com.accenture.springai_bootcamp_demo.dto;

/**
 * One concrete exercise in a generated practice plan.
 */
public record PracticeStepDto(
        String title,
        int durationMinutes,
        String instructions) {
}
