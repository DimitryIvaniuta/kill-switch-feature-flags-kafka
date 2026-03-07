package com.github.dimitryivaniuta.gateway.killswitch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Transactional outbox row used to reliably publish domain events to Kafka.
 */
@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

  /** Primary key. */
  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  /** Aggregate type (e.g. FeatureFlag). */
  @Column(name = "aggregate_type", nullable = false, length = 64)
  private String aggregateType;

  /** Aggregate id (e.g. flag name). */
  @Column(name = "aggregate_id", nullable = false, length = 200)
  private String aggregateId;

  /** Event type. */
  @Column(name = "event_type", nullable = false, length = 200)
  private String eventType;

  /** Event payload as JSON. */
  @Column(name = "payload_json", nullable = false, columnDefinition = "text")
  private String payloadJson;

  /** Creation time. */
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** When published (null if not published yet). */
  @Column(name = "published_at")
  private Instant publishedAt;

  /** Publish attempts. */
  @Column(name = "attempts", nullable = false)
  private int attempts;

  /** Last error (for debugging). */
  @Column(name = "last_error", length = 2000)
  private String lastError;
}
