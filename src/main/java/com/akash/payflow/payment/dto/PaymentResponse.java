package com.akash.payflow.payment.dto;

import com.akash.payflow.payment.entity.PaymentStatus;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record PaymentResponse(
		UUID id,
		BigDecimal amount,
		String currency,
		String reference,
		PaymentStatus status,
		Instant createdAt,
		Instant updatedAt
) {
}
