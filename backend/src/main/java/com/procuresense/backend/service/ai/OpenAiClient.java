package com.procuresense.backend.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.procuresense.backend.config.OpenAiProperties;
import com.procuresense.backend.model.AssistantChatRequest;
import com.procuresense.backend.model.AssistantChatResponse;
import com.procuresense.backend.model.Product;
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
import java.util.stream.Collectors;

@Service
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    private static final String RESPONSES_PATH = "/responses";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final OpenAiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenAiClient(OpenAiProperties properties,
                        RestClient.Builder restClientBuilder,
                        ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
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
        Map<String, Object> payload = buildReorderRequest(prediction, daysUntil);
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

    public Optional<String> generateBundleRationale(String sku,
                                                    String skuName,
                                                    String relatedSku,
                                                    String relatedName,
                                                    long coPurchaseCount) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        Map<String, Object> payload = bundleRequest(sku, skuName, relatedSku, relatedName, coPurchaseCount);
        try {
            JsonNode response = restClient.post()
                    .uri(RESPONSES_PATH)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
            String rationale = extractText(response);
            if (!StringUtils.hasText(rationale)) {
                return Optional.empty();
            }
            return Optional.of(rationale.trim());
        } catch (RestClientException ex) {
            log.warn("OpenAI bundle rationale failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, Object> buildReorderRequest(ReorderPrediction prediction, long daysUntil) {
        String systemPrompt = "You are a procurement copilot that explains reorder recommendations.";
        String userPrompt = buildUserPrompt(prediction, daysUntil);
        return Map.of(
                "model", properties.model(),
                "input", List.of(message("system", systemPrompt), message("user", userPrompt)),
                "temperature", properties.temperature(),
                "max_output_tokens", properties.maxOutputTokens()
        );
    }

    private Map<String, Object> bundleRequest(String sku,
                                              String skuName,
                                              String relatedSku,
                                              String relatedName,
                                              long coPurchaseCount) {
        String system = "You explain why two SKUs are often ordered together.";
        String friendly = StringUtils.hasText(relatedName) ? relatedName : relatedSku;
        String primaryFriendly = StringUtils.hasText(skuName) ? skuName : sku;
        String user = "Explain in 1 sentence why these SKUs should be bundled. " +
                "Mention warehouse/operations context when relevant.\n" +
                "Primary SKU: " + sku + '\n' +
                "Primary name: " + primaryFriendly + '\n' +
                "Related SKU: " + relatedSku + '\n' +
                "Related name: " + friendly + '\n' +
                "Co-purchase count: " + coPurchaseCount + '\n' +
                "Reply with plain text.";
        return Map.of(
                "model", properties.model(),
                "input", List.of(message("system", system), message("user", user)),
                "temperature", properties.temperature(),
                "max_output_tokens", properties.maxOutputTokens()
        );
    }

    private Map<String, Object> message(String role, String text) {
        return Map.of(
                "role", role,
                "content", List.of(Map.of("type", "input_text", "text", text))
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
                if (("output_text".equals(type) || "text".equals(type) || "input_text".equals(type)) && block.has("text")) {
                    return block.get("text").asText();
                }
            }
        }
        return null;
    }

    public Optional<AssistantChatResponse> generateAssistantResponse(String message,
                                                                     AssistantChatRequest.AssistantContext context,
                                                                     List<Product> catalog) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        Map<String, Object> payload = assistantRequest(message, context, catalog);
        try {
            JsonNode response = restClient.post()
                    .uri(RESPONSES_PATH)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
            String body = extractText(response);
            if (!StringUtils.hasText(body)) {
                return Optional.empty();
            }
            String normalized = normalizeJsonBlock(body);
            normalized = balanceJson(normalized);
            AssistantChatResponse parsed = objectMapper.readValue(normalized, AssistantChatResponse.class);
            return Optional.of(parsed);
        } catch (Exception ex) {
            log.warn("OpenAI assistant response failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private String normalizeJsonBlock(String body) {
        String cleaned = body.trim();

        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
            int closing = cleaned.lastIndexOf("```");
            if (closing >= 0) {
                cleaned = cleaned.substring(0, closing).trim();
            }
        } else {
            int firstFence = cleaned.indexOf("```");
            int lastFence = cleaned.lastIndexOf("```");
            if (firstFence >= 0 && lastFence > firstFence) {
                cleaned = cleaned.substring(firstFence + 3, lastFence).trim();
            }
        }

        if (cleaned.toLowerCase().startsWith("json")) {
            cleaned = cleaned.substring(4).trim();
        }

        cleaned = cleaned.replaceAll("^[`\"']+", "").replaceAll("[`\"']+$", "").trim();

        if (cleaned.toLowerCase().startsWith("json")) {
            cleaned = cleaned.substring(4).trim();
        }

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            cleaned = cleaned.substring(start, end + 1).trim();
        }

        return cleaned;
    }

    private String balanceJson(String json) {
        int braceDiff = 0;
        int bracketDiff = 0;
        for (char c : json.toCharArray()) {
            if (c == '{') {
                braceDiff++;
            } else if (c == '}') {
                braceDiff = Math.max(0, braceDiff - 1);
            } else if (c == '[') {
                bracketDiff++;
            } else if (c == ']') {
                bracketDiff = Math.max(0, bracketDiff - 1);
            }
        }
        StringBuilder builder = new StringBuilder(json);
        builder.append("]".repeat(Math.max(0, bracketDiff)));
        builder.append("}".repeat(Math.max(0, braceDiff)));
        return builder.toString();
    }

    private Map<String, Object> assistantRequest(String message,
                                                 AssistantChatRequest.AssistantContext context,
                                                 List<Product> catalog) {
        String system = "You are ProcureSense, a procurement assistant that recommends shopping kits using a provided catalog.";
        String catalogBlock = catalog.stream()
                .limit(20)
                .map(p -> p.getSku() + ": " + p.getName() + (StringUtils.hasText(p.getCategory()) ? " (" + p.getCategory() + ")" : ""))
                .collect(Collectors.joining("\n"));
        StringBuilder user = new StringBuilder();
        user.append("Scenario: ").append(message).append('\n');
        if (context != null) {
            if (StringUtils.hasText(context.selectedSku())) {
                user.append("Selected SKU: ").append(context.selectedSku()).append('\n');
            }
            if (StringUtils.hasText(context.orgType())) {
                user.append("Org type: ").append(context.orgType()).append('\n');
            }
            if (StringUtils.hasText(context.projectType())) {
                user.append("Project type: ").append(context.projectType()).append('\n');
            }
        }
        user.append("Catalog SKUs:\n").append(catalogBlock).append('\n');
        user.append("Respond with valid JSON: {\"replyText\":string, \"shoppingList\":[{\"sku\":string,\"name\":string,\"qty\":number,\"reason\":string}]}. Use only the provided SKUs and include 2-5 items.");
        return Map.of(
                "model", properties.model(),
                "input", List.of(message("system", system), message("user", user.toString())),
                "temperature", properties.temperature(),
                "max_output_tokens", properties.maxOutputTokens()
        );
    }
}
