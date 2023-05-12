package com.example.domain;

import com.example.domain.OrderEvent.OrderPlaced;

import java.math.BigDecimal;

import static com.example.domain.OrderStatus.CONFIRMED;
import static com.example.domain.OrderStatus.PLACED;
import static com.example.domain.OrderStatus.REJECTED;

public record Order(String id, String userId, String productId, int quantity, BigDecimal price,
                    OrderStatus status) {
  public static Order create(OrderPlaced orderPlaced) {
    return new Order(orderPlaced.orderId(), orderPlaced.userId(), orderPlaced.productId(), orderPlaced.quantity(), orderPlaced.price(), PLACED);
  }

  public Order asConfirmed() {
    return new Order(id, userId, productId, quantity, price, CONFIRMED);
  }

  public Order asRejected() {
    return new Order(id, userId, productId, quantity, price, REJECTED);
  }
}
