package com.example.application.choreography;

import com.example.application.Response;
import com.example.domain.InventoryCommand;
import com.example.domain.OrderEvent.OrderPlaced;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.spring.KalixClient;
import org.springframework.context.annotation.Profile;

import static com.example.domain.Inventory.INVENTORY_ID;

@Profile("choreography")
@Subscribe.EventSourcedEntity(value = OrderEntity.class, ignoreUnknown = true)
public class ReserveStocksAction extends Action {

  private final KalixClient kalixClient;

  public ReserveStocksAction(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  public Effect<Response> onEvent(OrderPlaced orderPlaced) {
    var reserveStocks = new InventoryCommand.ReserveStocks(orderPlaced.orderId(), orderPlaced.userId(), orderPlaced.productId(), orderPlaced.quantity(), orderPlaced.price());
    return effects().forward(kalixClient.patch("/inventory/" + INVENTORY_ID + "/reserve", reserveStocks, Response.class));
  }
}
