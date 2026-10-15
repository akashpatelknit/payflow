package com.akash.payflow.payment.entity;

public enum PaymentStatus {
	PENDING,
	PROCESSING,
	SUCCEEDED,
	FAILED;

	/**
	 * The single source of truth for allowed status transitions.
	 * The switch is exhaustive, so adding a status (e.g. UNKNOWN) will not compile until its rules are defined.
	 */
	public boolean canTransitionTo(PaymentStatus target) {
		return switch (this) {
			case PENDING -> target == PROCESSING;
			case PROCESSING -> target == SUCCEEDED || target == FAILED;
			case SUCCEEDED, FAILED -> false;
		};
	}
}
