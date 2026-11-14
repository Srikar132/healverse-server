package com.bytehealers.healverse.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableCaching
@EnableRetry
@Slf4j
public class AIServiceConfig {

    @Value("${app.ai.max-tokens:3000}")
    private Integer maxTokens;

    @Value("${app.ai.reasoning-effort:}")
    private String reasoningEffort;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    /**
     * Fail at startup, with a clear message, instead of at the first AI request: Spring would otherwise
     * boot happily with an unset GROQ_API_KEY and every AI call would later fail with a provider 401.
     */
    @PostConstruct
    void requireApiKey() {
        if (apiKey.isBlank() || apiKey.startsWith("${")) {
            throw new IllegalStateException("GROQ_API_KEY is not set. Add it to .env or the environment.");
        }
    }

    @Value("${app.ai.temperature:0.7}")
    private Double temperature;

    /**
     * Simple ChatClient configuration without advisors
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder()
                        .maxCompletionTokens(maxTokens)
                        .temperature(temperature)
                        .reasoningEffort(reasoningEffort.isBlank() ? null : reasoningEffort)
                        .build()
                )
                .build();
    }

    /**
     * Configure ObjectMapper for JSON processing with proper error handling
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, true);
        return mapper;
    }
}