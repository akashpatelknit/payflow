package com.akash.payflow.payment.exception;

import com.akash.payflow.common.exception.BadRequestException;

public class InvalidFailureReasonException extends BadRequestException {

	public InvalidFailureReasonException() {
		super("failureReason is only allowed when changing status to FAILED");
	}
}
