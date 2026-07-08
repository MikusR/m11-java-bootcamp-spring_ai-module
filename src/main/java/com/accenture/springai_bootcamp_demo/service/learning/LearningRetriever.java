package com.accenture.springai_bootcamp_demo.service.learning;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Small deterministic retrieval component for seeded bootcamp guidance.
 */
@Component
public class LearningRetriever {

    private static final int MAX_RESULTS = 3;

    public List<LearningKnowledgeBase.RetrievedLearningContext> retrieve(
            List<LearningKnowledgeBase.LearningTopic> selectedTopics,
            String query
    ) {
        Set<String> queryTerms = tokenize(query);
        return selectedTopics.stream()
                .map(topic -> score(topic, queryTerms))
                .sorted(Comparator
                        .comparingInt(ScoredTopic::score)
                        .reversed()
                        .thenComparing(scoredTopic -> scoredTopic.topic().title()))
                .limit(MAX_RESULTS)
                .map(scoredTopic -> new LearningKnowledgeBase.RetrievedLearningContext(
                        scoredTopic.topic().id(),
                        scoredTopic.topic().title(),
                        scoredTopic.matchedKeywords(),
                        scoredTopic.topic().article()))
                .toList();
    }

    private ScoredTopic score(LearningKnowledgeBase.LearningTopic topic, Set<String> queryTerms) {
        Set<String> matchedKeywords = new LinkedHashSet<>();
        Set<String> topicTerms = tokenize(topic.searchText());
        for (String term : queryTerms) {
            if (topicTerms.contains(term)) {
                matchedKeywords.add(term);
            }
        }
        int selectedTopicBoost = 10;
        int keywordScore = matchedKeywords.size() * 3;
        return new ScoredTopic(topic, selectedTopicBoost + keywordScore, List.copyOf(matchedKeywords));
    }

    private Set<String> tokenize(String value) {
        if (!StringUtils.hasText(value)) {
            return Set.of();
        }
        Set<String> tokens = new LinkedHashSet<>();
        Arrays.stream(value.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(token -> token.length() > 2)
                .forEach(tokens::add);
        return tokens;
    }

    private record ScoredTopic(
            LearningKnowledgeBase.LearningTopic topic,
            int score,
            List<String> matchedKeywords) {
    }
}
