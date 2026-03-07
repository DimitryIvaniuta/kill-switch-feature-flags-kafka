package com.github.dimitryivaniuta.gateway.killswitch.messaging;

import java.time.Instant;

/**
 * Kafka message representing a feature flag state change.
 *
 * @param flagName flag name
 * @param enabled enabled state
 * @param version monotonic version
 * @param changedAt event time
 * @param changedBy actor
 * @param reason reason
 */
public record FeatureFlagChangedEvent(
    String flagName,
    boolean enabled,
    long version,
    Instant changedAt,
    String changedBy,
    String reason
) {
}
