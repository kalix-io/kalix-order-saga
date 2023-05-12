package com.example.domain;

import java.math.BigDecimal;

public sealed interface InventoryCommand {
  record ReserveStocks(String orderId, String userId, String productId, Integer quantity,
                       BigDecimal price) implements InventoryCommand {
  }

  record AddProduct(String productId, Integer quantity) implements InventoryCommand {
  }
}
