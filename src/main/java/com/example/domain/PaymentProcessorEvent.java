package com.example.domain;

public sealed interface PaymentProcessorEvent {

  record PaymentSucceeded(String orderId) implements PaymentProcessorEvent {
  }

  record PaymentFailed(String orderId) implements PaymentProcessorEvent {
  }
}
