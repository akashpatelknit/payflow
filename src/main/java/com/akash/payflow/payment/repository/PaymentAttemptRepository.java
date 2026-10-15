package com.akash.payflow.payment.repository;

import com.akash.payflow.payment.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {

	Optional<PaymentAttempt> findFirstByPaymentIdOrderByAttemptNumberDesc(UUID paymentId);

	List<PaymentAttempt> findByPaymentIdOrderByAttemptNumberAsc(UUID paymentId);
}
