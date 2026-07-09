package com.accenture.springai_bootcamp_demo.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiConnectionProperties;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Fallback Spring AI OpenAI-compatible model wiring for OpenRouter.
 */
@Configuration(proxyBeanMethods = false)
public class OpenRouterSpringAiConfig {

    @Bean
    @ConditionalOnMissingBean
    OpenAiApi openAiApi(
            OpenAiConnectionProperties connectionProperties,
            OpenAiChatProperties chatProperties,
            ObjectProvider<RestClient.Builder> restClientBuilder,
            ObjectProvider<WebClient.Builder> webClientBuilder,
            ResponseErrorHandler responseErrorHandler
    ) {
        return OpenAiApi.builder()
                .baseUrl(resolve(chatProperties.getBaseUrl(), connectionProperties.getBaseUrl()))
                .apiKey(new SimpleApiKey(resolve(chatProperties.getApiKey(), connectionProperties.getApiKey())))
                .headers(headers(chatProperties, connectionProperties))
                .completionsPath(chatProperties.getCompletionsPath())
                .embeddingsPath("/v1/embeddings")
                .restClientBuilder(restClientBuilder.getIfAvailable(RestClient::builder))
                .webClientBuilder(webClientBuilder.getIfAvailable(WebClient::builder))
                .responseErrorHandler(responseErrorHandler)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    OpenAiChatModel openAiChatModel(
            OpenAiApi openAiApi,
            OpenAiChatProperties chatProperties,
            ObjectProvider<ToolCallingManager> toolCallingManager,
            ObjectProvider<ToolExecutionEligibilityPredicate> toolExecutionEligibilityPredicate,
            RetryTemplate retryTemplate,
            ObjectProvider<ObservationRegistry> observationRegistry
    ) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(chatProperties.getOptions())
                .toolCallingManager(toolCallingManager.getIfAvailable(() -> ToolCallingManager.builder().build()))
                .toolExecutionEligibilityPredicate(
                        toolExecutionEligibilityPredicate.getIfUnique(DefaultToolExecutionEligibilityPredicate::new))
                .retryTemplate(retryTemplate)
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .build();
    }

    private String resolve(String specificValue, String sharedValue) {
        return StringUtils.hasText(specificValue) ? specificValue : sharedValue;
    }

    private MultiValueMap<String, String> headers(
            OpenAiChatProperties chatProperties,
            OpenAiConnectionProperties connectionProperties
    ) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        String organizationId = resolve(chatProperties.getOrganizationId(), connectionProperties.getOrganizationId());
        String projectId = resolve(chatProperties.getProjectId(), connectionProperties.getProjectId());
        if (StringUtils.hasText(organizationId)) {
            headers.add("OpenAI-Organization", organizationId);
        }
        if (StringUtils.hasText(projectId)) {
            headers.add("OpenAI-Project", projectId);
        }
        return headers;
    }
}
