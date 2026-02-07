package com.procuresense.backend.service;

import com.procuresense.backend.model.Product;
import com.procuresense.backend.model.Purchase;
import com.procuresense.backend.model.ReorderPrediction;
import com.procuresense.backend.repository.PurchaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReorderInsightService {

    private final PurchaseRepository purchaseRepository;

    public ReorderInsightService(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    public List<ReorderPrediction> computePredictions(String orgId) {
        if (!StringUtils.hasText(orgId)) {
            return List.of();
        }
        List<Purchase> purchases = purchaseRepository.findByOrgIdOrderByProductSkuAscPurchasedAtAsc(orgId);
        Map<String, List<Purchase>> bySku = purchases.stream()
                .collect(Collectors.groupingBy(p -> p.getProduct().getSku(), LinkedHashMap::new, Collectors.toList()));

        List<ReorderPrediction> predictions = new ArrayList<>();
        for (Map.Entry<String, List<Purchase>> entry : bySku.entrySet()) {
            List<Purchase> skuPurchases = entry.getValue();
            if (skuPurchases.size() < 2) {
                continue;
            }
            skuPurchases.sort((a, b) -> a.getPurchasedAt().compareTo(b.getPurchasedAt()));
            List<Long> intervals = computeIntervals(skuPurchases);
            if (intervals.isEmpty()) {
                continue;
            }
            long medianDays = median(intervals);
            OffsetDateTime lastPurchase = skuPurchases.get(skuPurchases.size() - 1).getPurchasedAt();
            OffsetDateTime predictedReorder = lastPurchase.plusDays(medianDays);
            double confidence = computeConfidence(intervals);
            Product product = skuPurchases.get(0).getProduct();
            predictions.add(new ReorderPrediction(orgId, product.getSku(), product.getName(), lastPurchase,
                    medianDays, predictedReorder, confidence, null));
        }
        predictions.sort((a, b) -> {
            int cmp = a.predictedReorderAt().compareTo(b.predictedReorderAt());
            if (cmp != 0) {
                return cmp;
            }
            return Double.compare(b.confidence(), a.confidence());
        });
        return predictions;
    }

    private List<Long> computeIntervals(List<Purchase> purchases) {
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < purchases.size(); i++) {
            OffsetDateTime previous = purchases.get(i - 1).getPurchasedAt();
            OffsetDateTime current = purchases.get(i).getPurchasedAt();
            long days = Math.max(1, Duration.between(previous, current).toDays());
            intervals.add(days);
        }
        return intervals;
    }

    private long median(List<Long> intervals) {
        List<Long> sorted = new ArrayList<>(intervals);
        Collections.sort(sorted);
        int size = sorted.size();
        if (size % 2 == 1) {
            return sorted.get(size / 2);
        }
        long lower = sorted.get((size / 2) - 1);
        long upper = sorted.get(size / 2);
        return Math.round((lower + upper) / 2.0);
    }

    private double computeConfidence(List<Long> intervals) {
        if (intervals.isEmpty()) {
            return 0.0;
        }
        long min = Collections.min(intervals);
        long max = Collections.max(intervals);
        double variability = max == 0 ? 0 : (double) (max - min) / max;
        double coverage = Math.min(1.0, intervals.size() / 4.0);
        double stability = 1.0 - Math.min(1.0, variability);
        double confidence = 0.4 * coverage + 0.6 * stability;
        return Math.round(confidence * 100.0) / 100.0;
    }
}
