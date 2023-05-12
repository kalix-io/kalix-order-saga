package com.example.application;

import java.math.BigDecimal;

public record PlaceOrder(String userId, String productId, int quantity, BigDecimal price) {
}
