package com.example.application;

import com.example.application.Response.Failure;
import com.example.application.Response.Success;
import com.example.domain.PaymentProcessor;
import com.example.domain.PaymentProcessorCommand.MakePayment;
import com.example.domain.PaymentProcessorEvent;
import com.example.domain.PaymentProcessorEvent.PaymentFailed;
import com.example.domain.PaymentProcessorEvent.PaymentSucceeded;
import kalix.javasdk.annotations.EventHandler;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Id("id")
@TypeId("payment-processor")
@RequestMapping("/payment-processor/{id}")
public class PaymentProcessorEntity extends EventSourcedEntity<PaymentProcessor, PaymentProcessorEvent> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public PaymentProcessor emptyState() {
    return new PaymentProcessor();
  }

  @PostMapping
  public Effect<Response> makePayment(@RequestBody MakePayment makePayment) {
    logger.info("making payment: " + makePayment);
    String orderId = makePayment.orderId();
    if (orderId.equals("42")) {
      return effects()
          .emitEvent(new PaymentFailed(orderId))
          .thenReply(__ -> Failure.of("payment failed"));
    } else {
      return effects()
          .emitEvent(new PaymentSucceeded(orderId))
          .thenReply(__ -> Success.of("payment succeeded"));
    }
  }

  @EventHandler
  public PaymentProcessor onEvent(PaymentFailed failed) {
    return currentState();
  }

  @EventHandler
  public PaymentProcessor onEvent(PaymentSucceeded succeeded) {
    return currentState();
  }
}
