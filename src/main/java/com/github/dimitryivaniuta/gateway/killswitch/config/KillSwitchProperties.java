package com.github.dimitryivaniuta.gateway.killswitch.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the kill switch subsystem.
 */
@ConfigurationProperties(prefix = "killswitch")
public record KillSwitchProperties(
    Kafka kafka,
    Cache cache,
    Defaults defaults,
    Outbox outbox
) {

  /** Kafka-related configuration. */
  public record Kafka(String topic) {}

  /** Cache-related configuration. */
  public record Cache(Duration localTtl, Duration redisTtl) {}

  /** Default behavior configuration. */
  public record Defaults(boolean enabledIfMissing) {}

  /** Outbox publisher configuration. */
  public record Outbox(Duration publishFixedDelay, int maxAttempts) {}
}
