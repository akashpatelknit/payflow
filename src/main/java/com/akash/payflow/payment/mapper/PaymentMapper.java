package com.akash.payflow.payment.mapper;

import com.akash.payflow.payment.entity.Payment;
import com.akash.payflow.payment.entity.PaymentStatus;
import com.akash.payflow.payment.dto.CreatePaymentRequest;
import com.akash.payflow.payment.dto.PaymentResponse;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

	public Payment toEntity(CreatePaymentRequest request) {
		return Payment.builder()
				.amount(request.amount())
				.currency(request.currency())
				.reference(request.reference())
				.status(PaymentStatus.PENDING)
				.build();
	}

	public PaymentResponse toResponse(Payment payment) {
		return PaymentResponse.builder()
				.id(payment.getId())
				.amount(payment.getAmount())
				.currency(payment.getCurrency())
				.reference(payment.getReference())
				.status(payment.getStatus())
				.createdAt(payment.getCreatedAt())
				.updatedAt(payment.getUpdatedAt())
				.build();
	}
}
