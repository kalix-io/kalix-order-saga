package com.example.application.choreography;

import com.example.application.PaymentProcessorEntity;
import com.example.application.Response;
import com.example.domain.PaymentProcessorEvent.PaymentSucceeded;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.client.ComponentClient;
import org.springframework.context.annotation.Profile;

@Profile("choreography")
@Subscribe.EventSourcedEntity(value = PaymentProcessorEntity.class, ignoreUnknown = true)
public class ConfirmOrderAction extends Action {

  private final ComponentClient componentClient;

  public ConfirmOrderAction(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect<Response> onEvent(PaymentSucceeded paymentSucceeded) {
    return effects().forward(
      componentClient.forEventSourcedEntity(paymentSucceeded.orderId())
        .call(OrderEntity::confirmOrder));
  }
}
