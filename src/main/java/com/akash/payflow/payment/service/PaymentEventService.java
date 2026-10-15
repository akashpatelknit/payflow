package com.akash.payflow.payment.service;

import com.akash.payflow.payment.entity.Payment;
import com.akash.payflow.payment.entity.PaymentEvent;
import com.akash.payflow.payment.entity.PaymentEventType;
import com.akash.payflow.payment.entity.PaymentStatus;
import com.akash.payflow.payment.repository.PaymentEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes payment lifecycle events. Must join the caller's transaction so an event is committed
 * (or rolled back) together with the change it describes.
 */
@Service
@RequiredArgsConstructor
public class PaymentEventService {

	private final PaymentEventRepository paymentEventRepository;

	@Transactional(propagation = Propagation.MANDATORY)
	public void recordCreated(Payment payment) {
		paymentEventRepository.save(PaymentEvent.builder()
				.payment(payment)
				.eventType(PaymentEventType.PAYMENT_CREATED)
				.newStatus(payment.getStatus())
				.build());
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void recordStatusChanged(Payment payment, PaymentStatus previousStatus, String details) {
		paymentEventRepository.save(PaymentEvent.builder()
				.payment(payment)
				.eventType(PaymentEventType.STATUS_CHANGED)
				.previousStatus(previousStatus)
				.newStatus(payment.getStatus())
				.details(details)
				.build());
	}
}
