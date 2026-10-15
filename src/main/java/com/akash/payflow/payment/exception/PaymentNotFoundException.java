package com.akash.payflow.payment.exception;

import com.akash.payflow.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class PaymentNotFoundException extends ResourceNotFoundException {

	public PaymentNotFoundException(UUID id) {
		super("Payment not found: " + id);
	}
}
