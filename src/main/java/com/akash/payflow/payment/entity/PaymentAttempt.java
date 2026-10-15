package com.akash.payflow.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;

/**
 * One processing run of a payment. Opened as IN_PROGRESS when the payment enters PROCESSING and
 * completed exactly once when the payment reaches SUCCEEDED or FAILED.
 */
@Entity
@Table(
		// A payment cannot have two attempts with the same attempt number.
		// Example: payment_id = A, attempt_number = 1 can exist only once.
		// Different payments can each have their own attempt number 1.
		name = "payment_attempts",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_payment_attempts_payment_number", columnNames = {"payment_id", "attempt_number"})
)

// Ensures that attempt numbers are always positive: 1, 2, 3...
// Rejects attempt_number = 0 or a negative number at the database level.
@Check(name = "payment_attempts_number_positive", constraints = "attempt_number > 0")

// Keeps the status and completion timestamp consistent:
// IN_PROGRESS → completed_at must be NULL.
// SUCCEEDED or FAILED → completed_at must not be NULL.
// This prevents unfinished attempts from appearing completed
// and completed attempts from having no completion timestamp.
@Check(name = "payment_attempts_completion_consistent",
		constraints = "(status = 'IN_PROGRESS' and completed_at is null) "
				+ "or (status <> 'IN_PROGRESS' and completed_at is not null)")

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAttempt {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "payment_id", nullable = false, updatable = false,
			foreignKey = @ForeignKey(name = "fk_payment_attempts_payment"))
	private Payment payment;

	@Column(name = "attempt_number", nullable = false, updatable = false)
	private int attemptNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@Builder.Default  // tells Lombok to preserve a field's default value when creating an object using the @Builder pattern.
	@Setter(AccessLevel.NONE)
	private PaymentAttemptStatus status = PaymentAttemptStatus.IN_PROGRESS;

	@Column(name = "started_at", nullable = false, updatable = false)
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "failure_reason", length = 500)
	private String failureReason;

	public void complete(PaymentAttemptStatus outcome, String failureReason) {
		if (status != PaymentAttemptStatus.IN_PROGRESS) {
			throw new IllegalStateException("Attempt " + id + " is already completed");
		}
		if (outcome == PaymentAttemptStatus.IN_PROGRESS) {
			throw new IllegalArgumentException("Outcome must be SUCCEEDED or FAILED");
		}
		this.status = outcome;
		this.completedAt = Instant.now();
		this.failureReason = failureReason;
	}
}
