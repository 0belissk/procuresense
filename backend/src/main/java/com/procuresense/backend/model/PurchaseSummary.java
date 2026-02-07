package com.procuresense.backend.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PurchaseSummary(String orgId,
                              long totalOrders,
                              long totalLineItems,
                              long totalQuantity,
                              BigDecimal totalRevenue,
                              long totalSkus,
                              DateRange dateRange,
                              OffsetDateTime lastLoadedAt) {

    public record DateRange(OffsetDateTime start,
                            OffsetDateTime end) {
    }
}
