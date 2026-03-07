package com.github.dimitryivaniuta.gateway.killswitch.messaging;

import com.github.dimitryivaniuta.gateway.killswitch.cache.FeatureFlagCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer updating local/Redis cache upon flag change events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagChangedConsumer {

  private final FeatureFlagCache cache;

  /**
   * Consumes events and refreshes caches.
   */
  @KafkaListener(topics = "${killswitch.kafka.topic}")
  public void onMessage(FeatureFlagChangedEvent event) {
    if (event == null || event.flagName() == null) {
      return;
    }
    cache.put(event);
    log.info("Feature flag updated via Kafka: {} -> {} (v{})", event.flagName(), event.enabled(), event.version());
  }
}
