package com.akash.payflow.common.exception;

public abstract class ResourceNotFoundException extends RuntimeException {

	protected ResourceNotFoundException(String message) {
		super(message);
	}
}
