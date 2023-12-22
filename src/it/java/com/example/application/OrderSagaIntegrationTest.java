package com.example.application;

import com.example.domain.InventoryCommand.AddProduct;
import com.example.domain.Order;
import com.google.protobuf.any.Any;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.client.ComponentClient;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.example.domain.Inventory.INVENTORY_ID;
import static com.example.domain.OrderStatus.CONFIRMED;
import static com.example.domain.OrderStatus.REJECTED;
import static java.math.BigDecimal.TEN;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public abstract class OrderSagaIntegrationTest extends KalixIntegrationTestKitSupport {

  @Autowired
  private ComponentClient componentClient;

  private Duration timeout = Duration.of(10, SECONDS);

  @Test
  public void shouldConfirmOrder() {
    var userId = "bob";
    var productId = "1";
    var orderId = "1";
    var addProduct = new AddProduct(productId, 100);
    var placeOrder = new PlaceOrder(userId, productId, 1, TEN);

    addProductToInventory(addProduct);

    placeOrder(orderId, placeOrder);

    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          Order order = getOrder(orderId);
          assertThat(order.status()).isEqualTo(CONFIRMED);

          Integer availableQuantity = getProductAvailableQuantity(productId);
          assertThat(availableQuantity).isEqualTo(99);
        });
  }

  @Test
  public void shouldRejectOrderWhenInventoryIsEmpty() {
    var userId = "bob";
    var productId = "2";
    var orderId = "2";
    var placeOrder = new PlaceOrder(userId, productId, 1, TEN);

    placeOrder(orderId, placeOrder);

    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          Order order = getOrder(orderId);
          assertThat(order.status()).isEqualTo(REJECTED);
        });
  }

  @Test
  public void shouldRejectOrderWhenPaymentFails() {
    var userId = "bob";
    var productId = "3";
    var orderId = "42"; //failed payment
    var addProduct = new AddProduct(productId, 100);
    var placeOrder = new PlaceOrder(userId, productId, 1, TEN);

    addProductToInventory(addProduct);

    placeOrder(orderId, placeOrder);

    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          Order order = getOrder(orderId);
          assertThat(order.status()).isEqualTo(REJECTED);

          Integer availableQuantity = getProductAvailableQuantity(productId);
          assertThat(availableQuantity).isEqualTo(100);
        });
  }

  public void addProductToInventory(AddProduct addProduct) {
    execute(componentClient.forEventSourcedEntity(INVENTORY_ID)
        .call(InventoryEntity::add)
        .params(addProduct));
  }

  public Integer getProductAvailableQuantity(String productId) {
    return execute(componentClient.forEventSourcedEntity(INVENTORY_ID)
        .call(InventoryEntity::getQuantity)
        .params(productId));
  }

  public Order getOrder(String orderId) {
    return execute(componentClient.forWorkflow(orderId)
        .call(OrderWorkflow::get));
  }

  public void placeOrder(String orderId, PlaceOrder placeOrder) {
    execute(componentClient.forWorkflow(orderId)
        .call(OrderWorkflow::placeOrder)
        .params(placeOrder));
  }

  private <T> T execute(DeferredCall<Any, T> deferredCall) {
    try {
      return deferredCall.execute().toCompletableFuture().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }
}
