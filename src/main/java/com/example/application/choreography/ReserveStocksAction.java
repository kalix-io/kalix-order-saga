package com.example.application.choreography;

import com.example.application.InventoryEntity;
import com.example.application.Response;
import com.example.domain.InventoryCommand;
import com.example.domain.OrderEvent.OrderPlaced;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.client.ComponentClient;
import org.springframework.context.annotation.Profile;

import static com.example.domain.Inventory.INVENTORY_ID;

@Profile("choreography")
@Subscribe.EventSourcedEntity(value = OrderEntity.class, ignoreUnknown = true)
public class ReserveStocksAction extends Action {

  private final ComponentClient componentClient;

  public ReserveStocksAction(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect<Response> onEvent(OrderPlaced orderPlaced) {
    var reserveStocks = new InventoryCommand.ReserveStocks(orderPlaced.orderId(), orderPlaced.userId(), orderPlaced.productId(), orderPlaced.quantity(), orderPlaced.price());
    return effects().forward(
      componentClient.forEventSourcedEntity(INVENTORY_ID)
        .call(InventoryEntity::reserve)
        .params(reserveStocks));
  }
}
