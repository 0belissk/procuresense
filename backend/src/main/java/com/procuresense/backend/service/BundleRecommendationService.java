package com.procuresense.backend.service;

import com.procuresense.backend.model.BundleRecommendation;
import com.procuresense.backend.model.Purchase;
import com.procuresense.backend.repository.PurchaseRepository;
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

    private final PurchaseRepository purchaseRepository;

    public BundleRecommendationService(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    public List<BundleRecommendation> getBundlesForSku(String orgId, String sku) {
        if (!StringUtils.hasText(orgId) || !StringUtils.hasText(sku)) {
            return List.of();
        }
        String normalizedSku = sku.trim();
        List<Purchase> purchases = purchaseRepository.findByOrgIdOrderByProductSkuAscPurchasedAtAsc(orgId);
        Map<String, List<Purchase>> orders = purchases.stream()
                .collect(Collectors.groupingBy(this::orderKey, LinkedHashMap::new, Collectors.toList()));

        Map<String, BundleStats> counts = new HashMap<>();
        for (List<Purchase> orderPurchases : orders.values()) {
            boolean containsSku = orderPurchases.stream()
                    .anyMatch(p -> normalizedSku.equalsIgnoreCase(p.getProduct().getSku()));
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
                .map(stats -> new BundleRecommendation(orgId, normalizedSku, stats.relatedSku, stats.relatedName, stats.count))
                .collect(Collectors.toList());
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
