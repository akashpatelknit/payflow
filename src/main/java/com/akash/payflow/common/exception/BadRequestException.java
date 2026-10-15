package com.akash.payflow.common.exception;

public abstract class BadRequestException extends RuntimeException {

	protected BadRequestException(String message) {
		super(message);
	}
}
