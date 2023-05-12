package com.example.application.choreography;

import com.example.application.InventoryEntity;
import com.example.application.PaymentProcessorEntity;
import com.example.application.Response.Success;
import com.example.domain.PaymentProcessorEvent.PaymentFailed;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.client.ComponentClient;
import org.springframework.context.annotation.Profile;

import static com.example.domain.Inventory.INVENTORY_ID;

@Profile("choreography")
@Subscribe.EventSourcedEntity(value = PaymentProcessorEntity.class, ignoreUnknown = true)
public class CancelReservationAction extends Action {

  private final ComponentClient componentClient;

  public CancelReservationAction(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect<Success> onEvent(PaymentFailed paymentFailed) {
    var cancelReservationCall = componentClient.forEventSourcedEntity(INVENTORY_ID)
      .call(InventoryEntity::cancelReservation)
      .params(paymentFailed.orderId());
    return effects().forward(cancelReservationCall);
  }
}
