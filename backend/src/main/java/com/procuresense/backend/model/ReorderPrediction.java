package com.procuresense.backend.model;

import java.time.OffsetDateTime;

public record ReorderPrediction(String orgId,
                                String sku,
                                String productName,
                                OffsetDateTime lastPurchaseAt,
                                long medianDaysBetween,
                                OffsetDateTime predictedReorderAt,
                                double confidence,
                                long lastQuantity,
                                String explanation) {

    public ReorderPrediction withExplanation(String explanationText) {
        return new ReorderPrediction(orgId, sku, productName, lastPurchaseAt, medianDaysBetween,
                predictedReorderAt, confidence, lastQuantity, explanationText);
    }
}
