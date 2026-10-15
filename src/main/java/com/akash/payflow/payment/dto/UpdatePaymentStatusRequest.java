package com.akash.payflow.payment.dto;

import com.akash.payflow.payment.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdatePaymentStatusRequest(
		@NotNull PaymentStatus status
) {
}
