package com.example.domain;

import java.math.BigDecimal;

public sealed interface OrderEvent {

  record OrderPlaced(String orderId, String userId, String productId, int quantity,
                     BigDecimal price) implements OrderEvent {
  }

  record OrderConfirmed(String orderId) implements OrderEvent {
  }

  record OrderRejected(String orderId) implements OrderEvent {
  }
}
