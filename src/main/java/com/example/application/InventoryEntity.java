package com.example.application;

import com.example.application.Response.Success;
import com.example.domain.Inventory;
import com.example.domain.InventoryCommand.AddProduct;
import com.example.domain.InventoryCommand.ReserveStocks;
import com.example.domain.InventoryEvent;
import com.example.domain.InventoryEvent.ProductAdded;
import com.example.domain.InventoryEvent.StockReservationCancelled;
import com.example.domain.InventoryEvent.StocksReservationFailed;
import com.example.domain.InventoryEvent.StocksReserved;
import kalix.javasdk.annotations.EntityKey;
import kalix.javasdk.annotations.EntityType;
import kalix.javasdk.annotations.EventHandler;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;

import static java.util.Optional.ofNullable;

@EntityKey("id")
@EntityType("inventory")
@RequestMapping("/inventory/{id}")
public class InventoryEntity extends EventSourcedEntity<Inventory, InventoryEvent> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public Inventory emptyState() {
    return new Inventory(new HashMap<>(), new HashMap<>());
  }

  @PatchMapping("/add")
  public Effect<Response> add(@RequestBody AddProduct addProduct) {
    logger.info("processing: " + addProduct);
    ProductAdded productAdded = currentState().addProduct(addProduct);
    return effects()
        .emitEvent(productAdded)
        .thenReply(__ -> Success.of("product added"));
  }

  @PatchMapping("/reserve")
  public Effect<Response> reserve(@RequestBody ReserveStocks reserveStocks) {
    logger.info("processing: " + reserveStocks);
    InventoryEvent inventoryEvent = currentState().reserveStocks(reserveStocks);

    return effects()
        .emitEvent(inventoryEvent)
        .thenReply(__ -> toResponse(inventoryEvent));
  }

  private Response toResponse(InventoryEvent inventoryEvent) {
    if (inventoryEvent instanceof StocksReserved) {
      return Success.of("stocks reserved");
    } else if (inventoryEvent instanceof StocksReservationFailed) {
      return Response.Failure.of("stock reservation failed");
    } else {
      throw new IllegalStateException("Unexpected event: " + inventoryEvent);
    }
  }

  @PatchMapping("/cancel-reservation/{orderId}")
  public Effect<Success> cancelReservation(@PathVariable String orderId) {
    logger.info("cancelling reservation for orderId: " + orderId);
    InventoryEvent productAdded = currentState().cancelReservation(orderId);
    return effects()
        .emitEvent(productAdded)
        .thenReply(__ -> Success.of("reservation cancelled"));
  }

  @GetMapping("/product/{productId}")
  public Effect<Integer> getQuantity(@PathVariable String productId) {
    return ofNullable(currentState().stocks().get(productId))
        .map(quantity -> effects().reply(quantity))
        .orElseGet(() -> effects().error("product not found: " + productId));
  }

  @EventHandler
  public Inventory onEvent(ProductAdded productAdded) {
    return currentState().apply(productAdded);
  }


  @EventHandler
  public Inventory onEvent(StocksReserved stocksReserved) {
    return currentState().apply(stocksReserved);
  }

  @EventHandler
  public Inventory onEvent(StocksReservationFailed stocksReservationFailed) {
    return currentState().apply(stocksReservationFailed);
  }

  @EventHandler
  public Inventory onEvent(StockReservationCancelled stockReservationCancelled) {
    return currentState().apply(stockReservationCancelled);
  }
}
