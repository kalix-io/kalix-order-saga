package com.example.application.choreography;

import com.example.application.InventoryEntity;
import com.example.application.PaymentProcessorEntity;
import com.example.application.Response;
import com.example.domain.InventoryEvent.StockReservationCancelled;
import com.example.domain.InventoryEvent.StocksReservationFailed;
import com.example.domain.InventoryEvent.StocksReserved;
import com.example.domain.PaymentProcessorCommand.MakePayment;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.client.ComponentClient;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;

import static com.example.domain.PaymentProcessor.CARD_PAYMENT_PROCESSOR_ID;

@Profile("choreography")
@Subscribe.EventSourcedEntity(value = InventoryEntity.class, ignoreUnknown = true)
public class ProcessStockReservationAction extends Action {

  private final ComponentClient componentClient;

  public ProcessStockReservationAction(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect<Response> onEvent(StocksReserved stocksReserved) {
    var amount = stocksReserved.price().multiply(BigDecimal.valueOf(stocksReserved.quantity()));
    var makePayment = new MakePayment(stocksReserved.orderId(), stocksReserved.userId(), amount);
    return effects().forward(
      componentClient.forEventSourcedEntity(CARD_PAYMENT_PROCESSOR_ID)
        .call(PaymentProcessorEntity::makePayment)
        .params(makePayment));
  }

  public Effect<Response> onEvent(StockReservationCancelled stockReservationCancelled) {
    return rejectOrder(stockReservationCancelled.orderId());
  }

  public Effect<Response> onEvent(StocksReservationFailed stocksReservationFailed) {
    return rejectOrder(stocksReservationFailed.orderId());
  }

  private Effect<Response> rejectOrder(String orderId) {
    return effects().forward(
      componentClient.forEventSourcedEntity(orderId)
        .call(OrderEntity::rejectOrder));
  }
}
