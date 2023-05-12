package com.example.domain;

import com.example.domain.InventoryCommand.AddProduct;
import com.example.domain.InventoryCommand.ReserveStocks;
import com.example.domain.InventoryEvent.ProductAdded;
import com.example.domain.InventoryEvent.StockReservationCancelled;
import com.example.domain.InventoryEvent.StocksReservationFailed;
import com.example.domain.InventoryEvent.StocksReserved;

import java.util.Map;

public record Inventory(Map<String/*productId*/, Integer> stocks,
                        Map<String/*orderId*/, StockReservation> reservations) {

  public static final String INVENTORY_ID = "global-inventory";

  public ProductAdded addProduct(AddProduct addProduct) {
    return new ProductAdded(addProduct.productId(), addProduct.quantity());
  }

  public InventoryEvent reserveStocks(ReserveStocks reserveStocks) {
    Integer currentStock = stocks.get(reserveStocks.productId());
    if (currentStock == null || currentStock < reserveStocks.quantity()) {
      return new StocksReservationFailed(reserveStocks.orderId(), reserveStocks.userId(), reserveStocks.productId(), reserveStocks.quantity());
    } else {
      return new StocksReserved(reserveStocks.orderId(), reserveStocks.userId(), reserveStocks.productId(), reserveStocks.quantity(), reserveStocks.price());
    }
  }

  public StockReservationCancelled cancelReservation(String orderId) {
    return new StockReservationCancelled(orderId);
  }

  public Inventory apply(InventoryEvent inventoryEvent) {
    return switch (inventoryEvent) {
      case ProductAdded productAdded -> {
        stocks.merge(productAdded.productId(), productAdded.quantity(), (existingQuantity, added) -> existingQuantity + added);
        yield this;
      }
      case StocksReserved stocksReserved -> {
        stocks.compute(stocksReserved.productId(), (__, existingQuantity) -> existingQuantity - stocksReserved.quantity());
        reservations.put(stocksReserved.orderId(), new StockReservation(stocksReserved.orderId(), stocksReserved.productId(), stocksReserved.quantity()));
        yield this;
      }
      case StockReservationCancelled stockReservationCancelled -> {
        StockReservation stockReservation = reservations.get(stockReservationCancelled.orderId());
        stocks.compute(stockReservation.productId(), (__, existingQuantity) -> existingQuantity + stockReservation.quantity());
        reservations.remove(stockReservationCancelled.orderId());
        yield this;
      }
      case StocksReservationFailed __ -> this;
    };
  }
}
