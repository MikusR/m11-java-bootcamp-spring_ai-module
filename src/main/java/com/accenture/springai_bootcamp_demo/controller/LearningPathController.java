package com.accenture.springai_bootcamp_demo.controller;

import com.accenture.springai_bootcamp_demo.dto.LearningDiagnosisRequest;
import com.accenture.springai_bootcamp_demo.dto.LearningDiagnosisResponse;
import com.accenture.springai_bootcamp_demo.dto.LearningTopicDto;
import com.accenture.springai_bootcamp_demo.service.learning.LearningPathService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for the Learning Path Doctor diagnostic workflow.
 */
@RestController
@RequestMapping("/api/learning-path")
@AllArgsConstructor
public class LearningPathController {

    private final LearningPathService learningPathService;

    @GetMapping("/topics")
    public List<LearningTopicDto> topics() {
        return learningPathService.listTopics();
    }

    @PostMapping("/diagnose")
    public LearningDiagnosisResponse diagnose(@Valid @RequestBody LearningDiagnosisRequest request) {
        return learningPathService.diagnose(request);
    }
}
