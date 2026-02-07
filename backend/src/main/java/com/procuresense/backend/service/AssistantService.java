package com.procuresense.backend.service;

import com.procuresense.backend.model.AssistantChatRequest;
import com.procuresense.backend.model.AssistantChatResponse;
import com.procuresense.backend.model.Product;
import com.procuresense.backend.model.ShoppingListItem;
import com.procuresense.backend.repository.ProductRepository;
import com.procuresense.backend.service.ai.OpenAiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);
    private static final int PROMPT_CATALOG_LIMIT = 20;
    private static final int FALLBACK_ITEM_COUNT = 4;

    private final ProductRepository productRepository;
    private final OpenAiClient openAiClient;

    public AssistantService(ProductRepository productRepository, OpenAiClient openAiClient) {
        this.productRepository = productRepository;
        this.openAiClient = openAiClient;
    }

    public AssistantChatResponse handleChat(String orgId, AssistantChatRequest request, boolean cacheOnly) {
        if (!StringUtils.hasText(orgId)) {
            throw new IllegalArgumentException("X-Org-Id header is required");
        }
        List<Product> catalog = loadCatalog();
        List<Product> promptCatalog = catalog.stream()
                .sorted(Comparator.comparing(Product::getSku))
                .limit(PROMPT_CATALOG_LIMIT)
                .toList();

        Optional<AssistantChatResponse> aiResponse = cacheOnly
                ? Optional.empty()
                : openAiClient.generateAssistantResponse(
                request.message(),
                request.context(),
                promptCatalog);

        return aiResponse
                .map(response -> sanitizeResponse(response, promptCatalog))
                .orElseGet(() -> fallbackResponse(request, promptCatalog));
    }

    private List<Product> loadCatalog() {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            throw new IllegalStateException("Product catalog is empty. Load demo data first.");
        }
        return products;
    }

    private AssistantChatResponse sanitizeResponse(AssistantChatResponse response, List<Product> catalog) {
        Map<String, Product> catalogBySku = catalog.stream()
                .collect(Collectors.toMap(Product::getSku, p -> p));
        List<ShoppingListItem> sanitizedItems = response.shoppingList() == null ? List.of() : response.shoppingList().stream()
                .map(item -> sanitizeItem(item, catalogBySku))
                .filter(Objects::nonNull)
                .toList();

        if (sanitizedItems.isEmpty()) {
            log.info("Assistant response missing valid shopping list items. Falling back to deterministic kit.");
            return fallbackResponse(null, catalog);
        }

        String replyText = StringUtils.hasText(response.replyText())
                ? response.replyText().trim()
                : "Here is a reliable kit pulled from your catalog.";
        return new AssistantChatResponse(replyText, sanitizedItems);
    }

    private ShoppingListItem sanitizeItem(ShoppingListItem item, Map<String, Product> catalog) {
        if (item == null || !StringUtils.hasText(item.sku())) {
            return null;
        }
        Product product = catalog.get(item.sku());
        if (product == null) {
            return null;
        }
        int qty = item.qty() > 0 ? item.qty() : defaultQuantity(product);
        String name = StringUtils.hasText(item.name()) ? item.name() : product.getName();
        String reason = StringUtils.hasText(item.reason()) ? item.reason() : fallbackReason(product);
        return new ShoppingListItem(product.getSku(), name, qty, reason);
    }

    private AssistantChatResponse fallbackResponse(AssistantChatRequest request, List<Product> catalog) {
        List<ShoppingListItem> kit = catalog.stream()
                .sorted(Comparator.comparing(Product::getSku))
                .limit(FALLBACK_ITEM_COUNT)
                .map(product -> new ShoppingListItem(
                        product.getSku(),
                        product.getName(),
                        defaultQuantity(product),
                        fallbackReason(product)))
                .toList();
        String scenario = request != null && StringUtils.hasText(request.message())
                ? request.message().trim()
                : "your operations";
        String reply = "Cached kit for " + scenario + ". These staples keep receiving, storage, and safety covered.";
        log.info("Returning fallback assistant kit ({} items)", kit.size());
        return new AssistantChatResponse(reply, kit);
    }

    private int defaultQuantity(Product product) {
        String name = product.getName().toLowerCase();
        if (name.contains("glove") || name.contains("wipe") || name.contains("mask")) {
            return 4;
        }
        if (name.contains("tape") || name.contains("label")) {
            return 6;
        }
        return 2;
    }

    private String fallbackReason(Product product) {
        String category = product.getCategory();
        if (StringUtils.hasText(category)) {
            return "Core " + category.toLowerCase() + " item stocked for every kit.";
        }
        return "Reliable catalog staple for demo readiness.";
    }
}
