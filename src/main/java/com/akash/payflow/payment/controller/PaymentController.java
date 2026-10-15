package com.akash.payflow.payment.controller;

import com.akash.payflow.payment.service.PaymentService;
import com.akash.payflow.payment.dto.CreatePaymentRequest;
import com.akash.payflow.payment.dto.PaymentResponse;
import com.akash.payflow.payment.dto.UpdatePaymentStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService paymentService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PaymentResponse createPayment(
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
			@Valid @RequestBody CreatePaymentRequest request) {

		return paymentService.createPayment(request, idempotencyKey);
	}

	@PatchMapping("/{id}/status")
	public PaymentResponse updatePaymentStatus(@PathVariable UUID id,
			@Valid @RequestBody UpdatePaymentStatusRequest request) {
		return paymentService.transitionPayment(id, request.status(), request.failureReason());
	}

	@GetMapping("/{id}")
	public PaymentResponse getPaymentById(@PathVariable UUID id) {
		return paymentService.getPaymentById(id);
	}

	@GetMapping
	public Page<PaymentResponse> getAllPayments(Pageable pageable) {
		return paymentService.getAllPayments(pageable);
	}
}
