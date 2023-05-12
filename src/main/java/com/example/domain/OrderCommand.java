package com.example.domain;

import java.math.BigDecimal;

public interface OrderCommand {
  record PlaceOrder(String userId, String productId, int quantity, BigDecimal price) implements OrderCommand {
  }

  record RejectOrder(String reason) implements OrderCommand {
  }
}
