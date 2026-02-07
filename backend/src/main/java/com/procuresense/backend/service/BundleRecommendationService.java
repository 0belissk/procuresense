package com.procuresense.backend.service;

import com.procuresense.backend.model.BundleInsight;
import com.procuresense.backend.model.BundleRecommendation;
import com.procuresense.backend.model.Purchase;
import com.procuresense.backend.repository.BundleInsightRepository;
import com.procuresense.backend.repository.PurchaseRepository;
import com.procuresense.backend.service.ai.OpenAiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BundleRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(BundleRecommendationService.class);

    private final PurchaseRepository purchaseRepository;
    private final BundleInsightRepository bundleInsightRepository;
    private final OpenAiClient openAiClient;

    public BundleRecommendationService(PurchaseRepository purchaseRepository,
                                       BundleInsightRepository bundleInsightRepository,
                                       OpenAiClient openAiClient) {
        this.purchaseRepository = purchaseRepository;
        this.bundleInsightRepository = bundleInsightRepository;
        this.openAiClient = openAiClient;
    }

    public List<BundleRecommendation> getBundlesForSku(String orgId, String sku) {
        if (!StringUtils.hasText(orgId) || !StringUtils.hasText(sku)) {
            return List.of();
        }
        String normalizedSku = sku.trim();
        List<Purchase> purchases = purchaseRepository.findByOrgIdOrderByProductSkuAscPurchasedAtAsc(orgId);
        String primaryName = purchases.stream()
                .map(Purchase::getProduct)
                .filter(Objects::nonNull)
                .filter(product -> StringUtils.hasText(product.getSku()))
                .filter(product -> normalizedSku.equalsIgnoreCase(product.getSku()))
                .map(product -> StringUtils.hasText(product.getName()) ? product.getName() : normalizedSku)
                .findFirst()
                .orElse(normalizedSku);
        Map<String, List<Purchase>> orders = purchases.stream()
                .collect(Collectors.groupingBy(this::orderKey, LinkedHashMap::new, Collectors.toList()));

        Map<String, BundleStats> counts = new HashMap<>();
        for (List<Purchase> orderPurchases : orders.values()) {
            boolean containsSku = orderPurchases.stream()
                    .anyMatch(p -> p.getProduct() != null
                            && StringUtils.hasText(p.getProduct().getSku())
                            && normalizedSku.equalsIgnoreCase(p.getProduct().getSku()));
            if (!containsSku) {
                continue;
            }
            Set<String> seen = new HashSet<>();
            for (Purchase purchase : orderPurchases) {
                if (purchase.getProduct() == null || purchase.getProduct().getSku() == null) {
                    continue;
                }
                String otherSku = purchase.getProduct().getSku();
                if (normalizedSku.equalsIgnoreCase(otherSku)) {
                    continue;
                }
                if (!seen.add(otherSku)) {
                    continue;
                }
                counts.compute(otherSku, (key, existing) -> {
                    if (existing == null) {
                        existing = new BundleStats(otherSku, purchase.getProduct().getName());
                    }
                    existing.increment();
                    return existing;
                });
            }
        }

        return counts.values().stream()
                .sorted((a, b) -> Long.compare(b.count, a.count))
                .map(stats -> buildRecommendation(orgId, normalizedSku, primaryName, stats))
                .collect(Collectors.toList());
    }

    private BundleRecommendation buildRecommendation(String orgId, String sku, String skuName, BundleStats stats) {
        String fingerprint = sku + "|" + stats.relatedSku + "|" + stats.count;
        String rationale = bundleInsightRepository.findByOrgIdAndSkuAndRelatedSku(orgId, sku, stats.relatedSku)
                .filter(insight -> fingerprint.equals(insight.getFingerprint()))
                .map(insight -> {
                    log.debug("Bundle rationale cache hit for org={} sku={} related={}", orgId, sku, stats.relatedSku);
                    return insight.getRationaleText();
                })
                .orElseGet(() -> generateRationale(orgId, sku, skuName, stats.relatedSku, stats.relatedName, stats.count, fingerprint));
        return new BundleRecommendation(orgId, sku, stats.relatedSku, stats.relatedName, stats.count, rationale);
    }

    private String generateRationale(String orgId,
                                     String sku,
                                     String skuName,
                                     String relatedSku,
                                     String relatedName,
                                     long count,
                                     String fingerprint) {
        String promptFriendlyName = StringUtils.hasText(relatedName) ? relatedName : relatedSku;
        String primaryFriendlyName = StringUtils.hasText(skuName) ? skuName : sku;
        String fallback = String.format("%s is ordered alongside %s in %d historical orders, so stocking them together prevents misses.",
                promptFriendlyName, primaryFriendlyName, count);
        String generated = openAiClient.generateBundleRationale(sku, primaryFriendlyName, relatedSku, promptFriendlyName, count)
                .orElse(fallback);
        if (!openAiClient.isEnabled()) {
            log.debug("OpenAI disabled; using deterministic bundle rationale for org={} sku={} related={}", orgId, sku, relatedSku);
        }
        BundleInsight insight = bundleInsightRepository.findByOrgIdAndSkuAndRelatedSku(orgId, sku, relatedSku)
                .orElseGet(BundleInsight::new);
        insight.setOrgId(orgId);
        insight.setSku(sku);
        insight.setRelatedSku(relatedSku);
        insight.setCoPurchaseCount(count);
        insight.setRationaleText(generated);
        insight.setFingerprint(fingerprint);
        bundleInsightRepository.save(insight);
        return generated;
    }

    private String orderKey(Purchase purchase) {
        if (StringUtils.hasText(purchase.getOrderId())) {
            return purchase.getOrderId();
        }
        LocalDate date = purchase.getPurchasedAt() != null ? purchase.getPurchasedAt().toLocalDate() : LocalDate.MIN;
        return "DAY-" + date;
    }

    private static class BundleStats {
        private final String relatedSku;
        private final String relatedName;
        private long count;

        private BundleStats(String relatedSku, String relatedName) {
            this.relatedSku = relatedSku;
            this.relatedName = relatedName;
            this.count = 0;
        }

        private void increment() {
            this.count++;
        }
    }
}
