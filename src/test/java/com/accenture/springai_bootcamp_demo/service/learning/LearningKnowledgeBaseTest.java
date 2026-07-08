package com.accenture.springai_bootcamp_demo.service.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class LearningKnowledgeBaseTest {

    private final LearningKnowledgeBase knowledgeBase = new LearningKnowledgeBase(new LearningRetriever());

    @Test
    void allTopicsReturnsSeededTopics() {
        assertThat(knowledgeBase.allTopics())
                .extracting(LearningKnowledgeBase.LearningTopic::id)
                .containsExactly(
                        "java-core",
                        "oop",
                        "collections",
                        "exceptions",
                        "streams",
                        "concurrency",
                        "spring-boot",
                        "spring-ai",
                        "persistence",
                        "validation",
                        "testing",
                        "frontend");
        assertThat(knowledgeBase.allTopics())
                .allSatisfy(topic -> assertThat(topic.article()).contains("."));
    }

    @Test
    void retrievePrioritizesSelectedTopicsAndMatchingKeywords() {
        List<LearningKnowledgeBase.RetrievedLearningContext> context = knowledgeBase.retrieve(
                List.of("spring-ai", "testing", "frontend"),
                "I need help testing prompt workflows with ollama and fake model clients");

        assertThat(context).hasSize(3);
        assertThat(context.getFirst().topicId()).isEqualTo("spring-ai");
        assertThat(context.getFirst().matchedKeywords()).contains("prompt", "ollama");
        assertThat(context)
                .extracting(LearningKnowledgeBase.RetrievedLearningContext::topicId)
                .contains("testing");
    }

    @Test
    void findTopicsRejectsUnknownTopic() {
        assertThatThrownBy(() -> knowledgeBase.findTopics(List.of("missing")))
                .isInstanceOf(LearningTopicNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void retrieveUsesJavaArticlesAsGuidance() {
        List<LearningKnowledgeBase.RetrievedLearningContext> context = knowledgeBase.retrieve(
                List.of("java-core", "collections", "exceptions"),
                "records immutable collections and problem handlers");

        assertThat(context)
                .extracting(LearningKnowledgeBase.RetrievedLearningContext::topicId)
                .contains("java-core", "collections", "exceptions");
        assertThat(context.getFirst().guidance()).contains("Java Core");
    }
}
