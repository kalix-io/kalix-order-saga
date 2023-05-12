package com.example.domain;

import java.math.BigDecimal;

public interface PaymentProcessorCommand {

  record MakePayment(String orderId, String userId, BigDecimal amount) implements PaymentProcessorCommand {
  }
}
