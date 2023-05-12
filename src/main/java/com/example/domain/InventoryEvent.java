package com.example.domain;

import java.math.BigDecimal;

public sealed interface InventoryEvent {

  record ProductAdded(String productId, Integer quantity) implements InventoryEvent {
  }

  record StocksReserved(String orderId, String userId, String productId, Integer quantity,
                        BigDecimal price) implements InventoryEvent {
  }

  record StocksReservationFailed(String orderId, String userId, String productId,
                                 Integer quantity) implements InventoryEvent {
  }

  record StockReservationCancelled(String orderId) implements InventoryEvent {
  }
}
