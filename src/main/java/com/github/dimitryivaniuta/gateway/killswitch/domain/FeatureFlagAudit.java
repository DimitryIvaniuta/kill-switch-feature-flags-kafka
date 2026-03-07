package com.github.dimitryivaniuta.gateway.killswitch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Immutable audit log entry for each flag change.
 */
@Entity
@Table(name = "feature_flag_audit")
@Getter
@Setter
@NoArgsConstructor
public class FeatureFlagAudit {

  /** Audit id. */
  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  /** Flag name. */
  @Column(name = "flag_name", nullable = false, length = 200)
  private String flagName;

  /** Whether the feature is enabled. */
  @Column(name = "enabled", nullable = false)
  private boolean enabled;

  /** Version of the flag after applying the change. */
  @Column(name = "version", nullable = false)
  private long version;

  /** Action type. */
  @Enumerated(EnumType.STRING)
  @Column(name = "action", nullable = false, length = 32)
  private Action action;

  /** When it happened. */
  @Column(name = "changed_at", nullable = false)
  private Instant changedAt;

  /** Who changed it. */
  @Column(name = "changed_by", nullable = false, length = 200)
  private String changedBy;

  /** Why. */
  @Column(name = "reason", nullable = false, length = 1000)
  private String reason;

  /** Type of audit action. */
  public enum Action {
    CHANGE,
    ROLLBACK
  }
}
