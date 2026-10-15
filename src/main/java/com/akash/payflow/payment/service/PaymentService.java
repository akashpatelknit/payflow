package com.akash.payflow.payment.service;

import com.akash.payflow.payment.dto.CreatePaymentRequest;
import com.akash.payflow.payment.dto.PaymentResponse;
import com.akash.payflow.payment.entity.IdempotencyRecord;
import com.akash.payflow.payment.entity.PaymentAttemptStatus;
import com.akash.payflow.payment.entity.Payment;
import com.akash.payflow.payment.entity.PaymentStatus;
import com.akash.payflow.payment.exception.IdempotencyKeyConflictException;
import com.akash.payflow.payment.exception.InvalidFailureReasonException;
import com.akash.payflow.payment.exception.InvalidIdempotencyKeyException;
import com.akash.payflow.payment.exception.PaymentNotFoundException;
import com.akash.payflow.payment.mapper.PaymentMapper;
import com.akash.payflow.payment.repository.IdempotencyRecordRepository;
import com.akash.payflow.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

	private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 255;

	private final PaymentRepository paymentRepository;
	private final IdempotencyRecordRepository idempotencyRecordRepository;
	private final PaymentMapper paymentMapper;
	private final PaymentEventService paymentEventService;
	private final PaymentAttemptService paymentAttemptService;
	private final TransactionTemplate transactionTemplate;

	/**
	 * Creates a payment. When an idempotency key is supplied, a repeated request with the same payload
	 * returns the original payment, and the same key with a different payload is rejected.
	 * Transactions are programmatic here because the unique-violation fallback must run in a new
	 * transaction after the failed one has been rolled back.
	 */
	public PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey) {
		if (idempotencyKey == null) {
			return transactionTemplate.execute(status -> createNewPayment(request));
		}
		validateIdempotencyKey(idempotencyKey);
		String requestHash = hash(request);

		try {
			return transactionTemplate.execute(status -> createOrReplay(request, idempotencyKey, requestHash));
		} catch (DataIntegrityViolationException ex) {
			// A concurrent request with the same key committed first; return its result.
			return transactionTemplate.execute(status -> findExisting(idempotencyKey, requestHash)
					.orElseThrow(() -> ex));
		}
	}

	@Transactional
	public PaymentResponse transitionPayment(UUID id, PaymentStatus targetStatus, String failureReason) {
		if (failureReason != null && targetStatus != PaymentStatus.FAILED) {
			throw new InvalidFailureReasonException();
		}
		// Row lock: concurrent transitions on the same payment run one after another
		Payment payment = paymentRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new PaymentNotFoundException(id));
		PaymentStatus previousStatus = payment.getStatus();
		payment.transitionTo(targetStatus);

		// Attempt and event are written in this same transaction as the status change
		switch (targetStatus) {
			case PROCESSING -> paymentAttemptService.start(payment);
			case SUCCEEDED -> paymentAttemptService.complete(payment, PaymentAttemptStatus.SUCCEEDED, null);
			case FAILED -> paymentAttemptService.complete(payment, PaymentAttemptStatus.FAILED, failureReason);
			case PENDING -> { /* unreachable: no transition leads back to PENDING */ }
		}
		paymentEventService.recordStatusChanged(payment, previousStatus, failureReason);

		// Flush so updatedAt (set in @PreUpdate) is populated before building the response
		return paymentMapper.toResponse(paymentRepository.saveAndFlush(payment));
	}

	@Transactional(readOnly = true)
	public PaymentResponse getPaymentById(UUID id) {
		return paymentRepository.findById(id)
				.map(paymentMapper::toResponse)
				.orElseThrow(() -> new PaymentNotFoundException(id));
	}

	@Transactional(readOnly = true)
	public Page<PaymentResponse> getAllPayments(Pageable pageable) {
		return paymentRepository.findAll(pageable).map(paymentMapper::toResponse);
	}

	private PaymentResponse createOrReplay(CreatePaymentRequest request, String idempotencyKey, String requestHash) {
		Optional<PaymentResponse> existing = findExisting(idempotencyKey, requestHash);
		if (existing.isPresent()) {
			return existing.get();
		}

		Payment payment = paymentRepository.save(paymentMapper.toEntity(request));
		paymentEventService.recordCreated(payment);
		// Flush so a unique-key violation surfaces here, rolling back the payment insert too.
		idempotencyRecordRepository.saveAndFlush(IdempotencyRecord.builder()
				.idempotencyKey(idempotencyKey)
				.requestHash(requestHash)
				.payment(payment)
				.build());
		return paymentMapper.toResponse(payment);
	}

	private Optional<PaymentResponse> findExisting(String idempotencyKey, String requestHash) {
		return idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey).map(record -> {
			if (!record.getRequestHash().equals(requestHash)) {
				throw new IdempotencyKeyConflictException();
			}
			return paymentMapper.toResponse(record.getPayment());
		});
	}

	private PaymentResponse createNewPayment(CreatePaymentRequest request) {
		Payment payment = paymentRepository.save(paymentMapper.toEntity(request));
		paymentEventService.recordCreated(payment);
		return paymentMapper.toResponse(payment);
	}

	private void validateIdempotencyKey(String idempotencyKey) {
		try {
			UUID uuid = UUID.fromString(idempotencyKey);

			if (!uuid.toString().equalsIgnoreCase(idempotencyKey)) {
				throw new InvalidIdempotencyKeyException(
						"Idempotency-Key must be a valid canonical UUID");
			}
        }catch (IllegalArgumentException ex){
			throw new InvalidIdempotencyKeyException(
					"Idempotency-Key must be a valid canonical UUID");
		}

//		if (idempotencyKey.isBlank()) {
//			throw new InvalidIdempotencyKeyException("Idempotency-Key must not be blank");
//		}
//		if (idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
//			throw new InvalidIdempotencyKeyException(
//					"Idempotency-Key must be at most " + MAX_IDEMPOTENCY_KEY_LENGTH + " characters");
//		}
	}

	private String hash(CreatePaymentRequest request) {
		// stripTrailingZeros so 100.50 and 100.5 count as the same payload
		String canonical = request.amount().stripTrailingZeros().toPlainString()
				+ "|" + request.currency()
				+ "|" + request.reference();
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is not available", ex);
		}
	}
}
