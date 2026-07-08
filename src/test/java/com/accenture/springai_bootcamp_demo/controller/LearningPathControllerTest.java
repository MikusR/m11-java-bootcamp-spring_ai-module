package com.accenture.springai_bootcamp_demo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.accenture.springai_bootcamp_demo.dto.AgentTraceDto;
import com.accenture.springai_bootcamp_demo.dto.LearningDiagnosisDto;
import com.accenture.springai_bootcamp_demo.dto.LearningDiagnosisResponse;
import com.accenture.springai_bootcamp_demo.dto.LearningTopicDto;
import com.accenture.springai_bootcamp_demo.dto.PracticePlanDto;
import com.accenture.springai_bootcamp_demo.dto.PracticeStepDto;
import com.accenture.springai_bootcamp_demo.dto.RetrievedLearningContextDto;
import com.accenture.springai_bootcamp_demo.service.learning.LearningPathService;
import com.accenture.springai_bootcamp_demo.service.learning.LearningTopicNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LearningPathController.class)
class LearningPathControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LearningPathService learningPathService;

    @Test
    void topicsReturnsSeededTopics() throws Exception {
        when(learningPathService.listTopics()).thenReturn(List.of(
                new LearningTopicDto("spring-ai", "Spring AI", "Chat clients and prompts", "Use ChatClient for prompts.")));

        mockMvc.perform(get("/api/learning-path/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("spring-ai"))
                .andExpect(jsonPath("$[0].title").value("Spring AI"))
                .andExpect(jsonPath("$[0].article").value("Use ChatClient for prompts."));
    }

    @Test
    void diagnoseRejectsBlankGoal() throws Exception {
        mockMvc.perform(post("/api/learning-path/diagnose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerGoal": "",
                                  "struggles": "Prompts are confusing",
                                  "topics": ["spring-ai"],
                                  "timeAvailableMinutes": 45
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("learnerGoal")));
    }

    @Test
    void diagnoseRejectsEmptyTopicList() throws Exception {
        mockMvc.perform(post("/api/learning-path/diagnose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerGoal": "Learn Spring AI",
                                  "struggles": "Prompts are confusing",
                                  "topics": [],
                                  "timeAvailableMinutes": 45
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("topics")));
    }

    @Test
    void diagnoseReturnsWorkflowResponse() throws Exception {
        when(learningPathService.diagnose(any())).thenReturn(new LearningDiagnosisResponse(
                new LearningDiagnosisDto("Focus on orchestration.", List.of("Prompt context"), 70),
                List.of(new RetrievedLearningContextDto("spring-ai", "Spring AI", List.of("prompt"), "Guidance")),
                new PracticePlanDto(45, List.of(new PracticeStepDto("Trace flow", 15, "Read controller and service"))),
                "Practice one vertical slice.",
                List.of(new AgentTraceDto("DIAGNOSTICIAN", "Identify weak spots"))));

        mockMvc.perform(post("/api/learning-path/diagnose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerGoal": "Learn Spring AI",
                                  "struggles": "Prompts are confusing",
                                  "topics": ["spring-ai"],
                                  "timeAvailableMinutes": 45
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis.summary").value("Focus on orchestration."))
                .andExpect(jsonPath("$.retrievedContext[0].topicId").value("spring-ai"))
                .andExpect(jsonPath("$.practicePlan.steps[0].title").value("Trace flow"))
                .andExpect(jsonPath("$.agentTrace[0].agent").value("DIAGNOSTICIAN"));
    }

    @Test
    void diagnoseMapsUnknownTopicToBadRequest() throws Exception {
        doThrow(new LearningTopicNotFoundException("missing")).when(learningPathService).diagnose(any());

        mockMvc.perform(post("/api/learning-path/diagnose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerGoal": "Learn Spring AI",
                                  "struggles": "Prompts are confusing",
                                  "topics": ["missing"],
                                  "timeAvailableMinutes": 45
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown learning topic: missing"));
    }
}
