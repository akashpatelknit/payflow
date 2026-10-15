package com.akash.payflow.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePaymentRequest(
		@NotNull @Positive BigDecimal amount,
		@NotNull @Size(min = 3, max = 3) String currency,
		@NotBlank @Size(max = 100) String reference
) {
}
