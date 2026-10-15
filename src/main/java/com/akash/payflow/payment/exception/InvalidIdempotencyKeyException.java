package com.akash.payflow.payment.exception;

import com.akash.payflow.common.exception.BadRequestException;

public class InvalidIdempotencyKeyException extends BadRequestException {

	public InvalidIdempotencyKeyException(String message) {
		super(message);
	}
}
