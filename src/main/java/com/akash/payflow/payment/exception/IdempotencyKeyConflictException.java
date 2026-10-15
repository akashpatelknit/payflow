package com.akash.payflow.payment.exception;

import com.akash.payflow.common.exception.ConflictException;

public class IdempotencyKeyConflictException extends ConflictException {

	public IdempotencyKeyConflictException() {
		super("Idempotency-Key was already used with a different request payload");
	}
}
