package com.example.application;

import com.example.application.Response.Failure;
import com.example.application.Response.Success;
import com.example.domain.InventoryCommand.ReserveStocks;
import com.example.domain.Order;
import com.example.domain.OrderStatus;
import com.example.domain.PaymentProcessorCommand;
import com.google.protobuf.any.Any;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.client.ComponentClient;
import kalix.javasdk.workflow.Workflow;
import kalix.javasdk.workflow.Workflow.Effect.TransitionalEffect;
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
@Id("id")
@TypeId("order")
@RequestMapping("/order/{id}")
public class OrderWorkflow extends Workflow<Order> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  final private ComponentClient componentClient;

  public OrderWorkflow(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Override
  public WorkflowDef<Order> definition() {
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
    return componentClient.forEventSourcedEntity(INVENTORY_ID)
      .call(InventoryEntity::reserve)
      .params(reserveStocksCommand);
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
    return componentClient.forEventSourcedEntity(CARD_PAYMENT_PROCESSOR_ID)
      .call(PaymentProcessorEntity::makePayment)
      .params(makePayment);
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
    return componentClient.forEventSourcedEntity(INVENTORY_ID)
      .call(InventoryEntity::cancelReservation)
      .params(order.id());
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
      String orderId = commandContext().workflowId();
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
