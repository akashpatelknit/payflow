package com.akash.payflow.payment.dto;

import com.akash.payflow.payment.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePaymentStatusRequest(
		@NotNull PaymentStatus status,
		@Size(max = 500) String failureReason
) {
}
