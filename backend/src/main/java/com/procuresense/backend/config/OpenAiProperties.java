package com.procuresense.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(boolean enabled,
                               String apiKey,
                               String baseUrl,
                               String model,
                               double temperature,
                               int maxOutputTokens) {

    public boolean isConfigured() {
        return enabled && StringUtils.hasText(apiKey);
    }
}
