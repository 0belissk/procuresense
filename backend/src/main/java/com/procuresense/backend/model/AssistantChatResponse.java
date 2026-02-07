package com.procuresense.backend.model;

import java.util.List;

public record AssistantChatResponse(String replyText,
                                    List<ShoppingListItem> shoppingList) {
}
