package com.akash.payflow.payment.exception;

import com.akash.payflow.common.exception.ConflictException;
import com.akash.payflow.payment.entity.PaymentStatus;

import java.util.UUID;

public class InvalidPaymentStatusTransitionException extends ConflictException {

	public InvalidPaymentStatusTransitionException(UUID paymentId, PaymentStatus from, PaymentStatus to) {
		super("Cannot change payment " + paymentId + " from " + from + " to " + to);
	}
}
