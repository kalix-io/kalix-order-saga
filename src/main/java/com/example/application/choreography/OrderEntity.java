package com.example.application.choreography;

import com.example.application.PlaceOrder;
import com.example.application.Response;
import com.example.application.Response.Success;
import com.example.domain.Order;
import com.example.domain.OrderEvent;
import com.example.domain.OrderEvent.OrderConfirmed;
import com.example.domain.OrderEvent.OrderPlaced;
import com.example.domain.OrderEvent.OrderRejected;
import kalix.javasdk.annotations.EntityKey;
import kalix.javasdk.annotations.EntityType;
import kalix.javasdk.annotations.EventHandler;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Profile("choreography")
@EntityKey("id")
@EntityType("order")
@RequestMapping("/order/{id}")
public class OrderEntity extends EventSourcedEntity<Order, OrderEvent> {

  @PostMapping
  public Effect<Response> placeOrder(@PathVariable String id, @RequestBody PlaceOrder placeOrder) {
    OrderPlaced orderPlaced = new OrderPlaced(id, placeOrder.userId(), placeOrder.productId(), placeOrder.quantity(), placeOrder.price());
    return effects()
        .emitEvent(orderPlaced)
        .thenReply(__ -> Success.of("order placed"));
  }

  @PatchMapping("/reject")
  public Effect<Response> rejectOrder(@PathVariable String id) {
    return effects()
        .emitEvent(new OrderRejected(id))
        .thenReply(__ -> Success.of("order placed"));
  }

  @PatchMapping("/confirm")
  public Effect<Response> confirmOrder(@PathVariable String id) {
    return effects()
        .emitEvent(new OrderConfirmed(id))
        .thenReply(__ -> Success.of("order placed"));
  }

  @GetMapping
  public Effect<Order> get() {
    return effects().reply(currentState());
  }

  @EventHandler
  public Order onEvent(OrderPlaced orderPlaced) {
    return Order.create(orderPlaced);
  }

  @EventHandler
  public Order onEvent(OrderRejected rejected) {
    return currentState().asRejected();
  }

  @EventHandler
  public Order onEvent(OrderConfirmed confirmed) {
    return currentState().asConfirmed();
  }
}
