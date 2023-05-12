package com.example.application.choreography;

import com.example.application.PaymentProcessorEntity;
import com.example.application.Response;
import com.example.domain.PaymentProcessorEvent.PaymentFailed;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.spring.KalixClient;
import org.springframework.context.annotation.Profile;

import static com.example.domain.Inventory.INVENTORY_ID;

@Profile("choreography")
@Subscribe.EventSourcedEntity(value = PaymentProcessorEntity.class, ignoreUnknown = true)
public class CancelReservationAction extends Action {

  private final KalixClient kalixClient;

  public CancelReservationAction(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  public Effect<Response> onEvent(PaymentFailed paymentFailed) {
    var cancelReservationCall = kalixClient.patch("/inventory/" + INVENTORY_ID + "/cancel-reservation/" + paymentFailed.orderId(), Response.class);
    return effects().forward(cancelReservationCall);
  }
}
