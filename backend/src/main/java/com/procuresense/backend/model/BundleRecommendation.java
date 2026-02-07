package com.procuresense.backend.model;

public record BundleRecommendation(String orgId,
                                   String sku,
                                   String relatedSku,
                                   String relatedName,
                                   long coPurchaseCount,
                                   String rationale) {
}
