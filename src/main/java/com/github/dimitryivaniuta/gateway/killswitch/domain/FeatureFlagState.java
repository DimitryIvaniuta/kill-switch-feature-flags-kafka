package com.github.dimitryivaniuta.gateway.killswitch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Current state of a feature flag.
 */
@Entity
@Table(name = "feature_flag_state")
@Getter
@Setter
@NoArgsConstructor
public class FeatureFlagState {

  /**
   * Flag name (business key).
   */
  @Id
  @Column(name = "flag_name", nullable = false, length = 200)
  private String flagName;

  /** Whether the feature is enabled. */
  @Column(name = "enabled", nullable = false)
  private boolean enabled;

  /**
   * Monotonic version per flag. Used for idempotency and ordering.
   */
  @Column(name = "version", nullable = false)
  private long version;

  /** Last change time. */
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Actor. */
  @Column(name = "updated_by", nullable = false, length = 200)
  private String updatedBy;

  /** Reason for the change. */
  @Column(name = "reason", nullable = false, length = 1000)
  private String reason;
}
