package com.accenture.springai_bootcamp_demo.dto;

import java.util.List;

/**
 * Structured diagnosis extracted from the diagnostician agent's response.
 */
public record LearningDiagnosisDto(
        String summary,
        List<String> weakSpots,
        int confidenceScore) {
}
