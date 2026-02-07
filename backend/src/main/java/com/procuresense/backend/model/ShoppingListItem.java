package com.procuresense.backend.model;

public record ShoppingListItem(String sku,
                               String name,
                               int qty,
                               String reason) {
}
