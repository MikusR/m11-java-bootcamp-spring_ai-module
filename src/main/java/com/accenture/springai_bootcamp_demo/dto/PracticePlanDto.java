package com.accenture.springai_bootcamp_demo.dto;

import java.util.List;

/**
 * Time-boxed practice plan generated for the learner.
 */
public record PracticePlanDto(
        int timeBoxMinutes,
        List<PracticeStepDto> steps) {
}
