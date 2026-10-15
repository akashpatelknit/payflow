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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit record of something that happened to a payment. Never updated after insert.
 */
@Entity
@Table(
		name = "payment_events",
		indexes = @Index(name = "idx_payment_events_payment_created", columnList = "payment_id, created_at")
)
@Check(name = "payment_events_created_has_no_previous_status",
		constraints = "event_type <> 'PAYMENT_CREATED' or previous_status is null")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "payment_id", nullable = false, updatable = false,
			foreignKey = @ForeignKey(name = "fk_payment_events_payment"))
	private Payment payment;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, updatable = false, length = 30)
	private PaymentEventType eventType;

	/** Null for PAYMENT_CREATED. */
	@Enumerated(EnumType.STRING)
	@Column(name = "previous_status", updatable = false, length = 20)
	private PaymentStatus previousStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "new_status", nullable = false, updatable = false, length = 20)
	private PaymentStatus newStatus;

	@Column(updatable = false, length = 500)
	private String details;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void onCreate() {
		this.createdAt = Instant.now();
	}
}
