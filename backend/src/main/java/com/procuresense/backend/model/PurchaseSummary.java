package com.procuresense.backend.model;

import java.math.BigDecimal;

public record PurchaseSummary(long totalOrders,
                              long totalLineItems,
                              long totalQuantity,
                              BigDecimal totalRevenue) {
}
