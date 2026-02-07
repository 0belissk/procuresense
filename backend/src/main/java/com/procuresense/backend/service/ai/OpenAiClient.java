package com.procuresense.backend.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.procuresense.backend.config.OpenAiProperties;
import com.procuresense.backend.model.ReorderPrediction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    private static final String RESPONSES_PATH = "/responses";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final OpenAiProperties properties;
    private final RestClient restClient;

    public OpenAiClient(OpenAiProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        if (properties.isConfigured()) {
            this.restClient = restClientBuilder
                    .baseUrl(properties.baseUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
            log.info("OpenAI client enabled with model {}", properties.model());
        } else {
            this.restClient = null;
            log.info("OpenAI client disabled. Set OPENAI_API_KEY and openai.enabled=true to enable explanations.");
        }
    }

    public boolean isEnabled() {
        return restClient != null;
    }

    public Optional<String> generateReorderExplanation(ReorderPrediction prediction, long daysUntil) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        Map<String, Object> payload = buildRequest(prediction, daysUntil);
        try {
            JsonNode response = restClient.post()
                    .uri(RESPONSES_PATH)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
            String explanation = extractText(response);
            if (!StringUtils.hasText(explanation)) {
                return Optional.empty();
            }
            return Optional.of(explanation.trim());
        } catch (RestClientException ex) {
            log.warn("OpenAI reorder explanation failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, Object> buildRequest(ReorderPrediction prediction, long daysUntil) {
        String systemPrompt = "You are a procurement copilot that explains reorder recommendations.";
        String userPrompt = buildUserPrompt(prediction, daysUntil);
        return Map.of(
                "model", properties.model(),
                "input", List.of(message("system", systemPrompt), message("user", userPrompt)),
                "temperature", properties.temperature(),
                "max_output_tokens", properties.maxOutputTokens()
        );
    }

    private Map<String, Object> message(String role, String text) {
        return Map.of(
                "role", role,
                "content", List.of(Map.of("type", "text", "text", text))
        );
    }

    private String buildUserPrompt(ReorderPrediction prediction, long daysUntil) {
        String product = StringUtils.hasText(prediction.productName()) ? prediction.productName() : prediction.sku();
        String confidencePercent = String.format("%.0f", prediction.confidence() * 100);
        String lastPurchase = DATE_FORMATTER.format(prediction.lastPurchaseAt().toLocalDate());
        String predicted = DATE_FORMATTER.format(prediction.predictedReorderAt().toLocalDate());
        return "Generate a concise 1-2 sentence explanation for why this SKU is due for reorder. " +
                "Use only the provided facts, mention the cadence, last purchase, and predicted date.\n" +
                "SKU: " + prediction.sku() + '\n' +
                "Product: " + product + '\n' +
                "Typical cadence (days): " + prediction.medianDaysBetween() + '\n' +
                "Last purchase: " + lastPurchase + '\n' +
                "Predicted reorder date: " + predicted + '\n' +
                "Days until predicted need: " + daysUntil + '\n' +
                "Confidence percent: " + confidencePercent + '\n' +
                "Reply with plain text only.";
    }

    private String extractText(JsonNode response) {
        if (response == null) {
            return null;
        }
        JsonNode output = response.get("output");
        if (output == null || !output.isArray()) {
            return null;
        }
        for (JsonNode message : output) {
            JsonNode content = message.get("content");
            if (content == null || !content.isArray()) {
                continue;
            }
            for (JsonNode block : content) {
                String type = block.path("type").asText();
                if (("output_text".equals(type) || "text".equals(type)) && block.has("text")) {
                    return block.get("text").asText();
                }
            }
        }
        return null;
    }
}
