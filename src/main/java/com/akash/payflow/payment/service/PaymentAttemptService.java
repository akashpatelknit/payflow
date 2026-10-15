package com.akash.payflow.payment.service;

import com.akash.payflow.payment.entity.Payment;
import com.akash.payflow.payment.entity.PaymentAttempt;
import com.akash.payflow.payment.entity.PaymentAttemptStatus;
import com.akash.payflow.payment.repository.PaymentAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Manages processing attempts. Callers must hold the payment's row lock (findByIdForUpdate) so
 * attempt numbers are assigned one at a time; the unique constraint is the backstop.
 */
@Service
@RequiredArgsConstructor
public class PaymentAttemptService {

	private static final Logger log = LoggerFactory.getLogger(PaymentAttemptService.class);

	private final PaymentAttemptRepository paymentAttemptRepository;

	@Transactional(propagation = Propagation.MANDATORY)
	public PaymentAttempt start(Payment payment) {
		int nextNumber = paymentAttemptRepository.findFirstByPaymentIdOrderByAttemptNumberDesc(payment.getId())
				.map(last -> last.getAttemptNumber() + 1)
				.orElse(1);
		return paymentAttemptRepository.save(PaymentAttempt.builder()
				.payment(payment)
				.attemptNumber(nextNumber)
				.startedAt(Instant.now())
				.build());
	}

	/**
	 * Completes the open attempt. A PROCESSING payment created before attempts existed has none;
	 * that is tolerated (and logged) so the existing status API keeps working for legacy rows.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void complete(Payment payment, PaymentAttemptStatus outcome, String failureReason) {
		paymentAttemptRepository.findFirstByPaymentIdOrderByAttemptNumberDesc(payment.getId())
				.filter(attempt -> attempt.getStatus() == PaymentAttemptStatus.IN_PROGRESS)
				.ifPresentOrElse(
						attempt -> attempt.complete(outcome, failureReason),
						() -> log.warn("Payment {} has no in-progress attempt to complete", payment.getId()));
	}
}
