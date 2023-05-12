package com.example.application;

import com.example.application.Response.Failure;
import com.example.application.Response.Success;
import com.example.domain.InventoryCommand.ReserveStocks;
import com.example.domain.Order;
import com.example.domain.OrderStatus;
import com.example.domain.PaymentProcessorCommand;
import com.google.protobuf.any.Any;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.annotations.EntityKey;
import kalix.javasdk.annotations.EntityType;
import kalix.javasdk.workflowentity.WorkflowEntity;
import kalix.javasdk.workflowentity.WorkflowEntity.Effect.TransitionalEffect;
import kalix.spring.KalixClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;

import static com.example.domain.Inventory.INVENTORY_ID;
import static com.example.domain.PaymentProcessor.CARD_PAYMENT_PROCESSOR_ID;
import static java.time.Duration.ofSeconds;

@Profile("orchestration")
@EntityKey("id")
@EntityType("order")
@RequestMapping("/order/{id}")
public class OrderWorkflow extends WorkflowEntity<Order> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  final private KalixClient kalixClient;

  public OrderWorkflow(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  @Override
  public Workflow<Order> definition() {
    Step reserveStocks = step("reserve-stocks")
        .call(this::reserveInventoryStocks)
        .andThen(Response.class, this::moveToPaymentOrReject);

    Step makePayment = step("make-payment")
        .call(this::callPaymentProcessor)
        .andThen(Response.class, this::confirmOrderOrRollback);

    Step cancelReservation = step("cancel-reservation")
        .call(this::cancelStockReservation)
        .andThen(Success.class, this::rejectOrder);

    return workflow()
        .timeout(ofSeconds(20))
        .defaultStepTimeout(ofSeconds(5))
        .addStep(reserveStocks)
        .addStep(makePayment)
        .addStep(cancelReservation);
  }

  private DeferredCall<Any, Response> reserveInventoryStocks() {
    var order = currentState();
    var reserveStocksCommand = new ReserveStocks(order.id(), order.userId(), order.productId(), order.quantity(), order.price());
    return kalixClient.patch("/inventory/" + INVENTORY_ID + "/reserve", reserveStocksCommand, Response.class);
  }

  private TransitionalEffect<Void> moveToPaymentOrReject(Response response) {
    return switch (response) {
      case Failure __ -> {
        logger.info("stock reservation failed");
        yield effects().updateState(currentState().asRejected()).end();
      }
      case Success __ -> effects().transitionTo("make-payment");
    };
  }

  private DeferredCall<Any, Response> callPaymentProcessor() {
    var order = currentState();
    var amount = order.price().multiply(BigDecimal.valueOf(order.quantity()));
    var makePayment = new PaymentProcessorCommand.MakePayment(order.id(), order.userId(), amount);
    return kalixClient.post("/payment-processor/" + CARD_PAYMENT_PROCESSOR_ID, makePayment, Response.class);
  }

  private TransitionalEffect<Void> confirmOrderOrRollback(Response response) {
    return switch (response) {
      case Failure __ -> {
        logger.info("payment failed");
        yield effects()
            .updateState(currentState().asRejected())
            .transitionTo("cancel-reservation");
      }
      case Success __ -> {
        logger.info("payment succeeded");
        yield effects()
            .updateState(currentState().asConfirmed())
            .end();
      }
    };
  }

  private DeferredCall<Any, Success> cancelStockReservation() {
    var order = currentState();
    return kalixClient.patch("/inventory/" + INVENTORY_ID + "/cancel-reservation/" + order.id(), Success.class);
  }

  private TransitionalEffect<Void> rejectOrder(Success success) {
    logger.info("reservation cancellation succeeded");
    return effects().updateState(currentState().asRejected()).end();
  }

  @PostMapping
  public Effect<Response> placeOrder(@RequestBody PlaceOrder placeOrder) {
    if (currentState() != null) {
      return effects().error("order already placed");
    } else {
      String orderId = commandContext().entityId();
      Order order = new Order(orderId, placeOrder.userId(), placeOrder.productId(), placeOrder.quantity(), placeOrder.price(), OrderStatus.PLACED);

      return effects()
          .updateState(order)
          .transitionTo("reserve-stocks")
          .thenReply(Success.of("order placed"));
    }
  }

  @GetMapping
  public Effect<Order> get() {
    return effects().reply(currentState());
  }

}
