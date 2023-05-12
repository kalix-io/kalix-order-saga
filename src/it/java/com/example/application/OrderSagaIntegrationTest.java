package com.example.application;

import com.example.domain.InventoryCommand.AddProduct;
import com.example.domain.Order;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.example.domain.Inventory.INVENTORY_ID;
import static com.example.domain.OrderStatus.CONFIRMED;
import static com.example.domain.OrderStatus.REJECTED;
import static java.math.BigDecimal.TEN;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public abstract class OrderSagaIntegrationTest extends KalixIntegrationTestKitSupport {

  @Autowired
  private WebClient webClient;

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
    Response.Success response = webClient
        .patch()
        .uri("/inventory/" + INVENTORY_ID + "/add")
        .bodyValue(addProduct)
        .retrieve()
        .bodyToMono(Response.Success.class)
        .block(timeout);

    assertThat(response.message()).isEqualTo("product added");
  }

  public Integer getProductAvailableQuantity(String productId) {
    return webClient
        .get()
        .uri("/inventory/" + INVENTORY_ID + "/product/" + productId)
        .retrieve()
        .bodyToMono(Integer.class)
        .block(timeout);
  }

  public Order getOrder(String orderId) {
    Order order = webClient
        .get()
        .uri("/order/" + orderId)
        .retrieve()
        .bodyToMono(Order.class)
        .block(timeout);
    return order;
  }

  public void placeOrder(String orderId, PlaceOrder placeOrder) {
    Response.Success response = webClient
        .post()
        .uri("/order/" + orderId)
        .bodyValue(placeOrder)
        .retrieve()
        .bodyToMono(Response.Success.class)
        .block(timeout);

    assertThat(response.message()).isEqualTo("order placed");
  }
}
