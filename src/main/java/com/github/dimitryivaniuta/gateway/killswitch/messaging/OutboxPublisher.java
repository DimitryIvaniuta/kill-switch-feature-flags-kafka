package com.github.dimitryivaniuta.gateway.killswitch.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dimitryivaniuta.gateway.killswitch.config.KillSwitchProperties;
import com.github.dimitryivaniuta.gateway.killswitch.repo.OutboxEventRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Polls transactional outbox and publishes events to Kafka.
 *
 * Designed for partial outage tolerance: if Kafka is down, events remain in DB
 * and will be retried.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

  private final OutboxEventRepository outboxRepository;
  private final KafkaTemplate<String, FeatureFlagChangedEvent> kafkaTemplate;
  private final KillSwitchProperties properties;
  private final ObjectMapper objectMapper;

  /**
   * Publish pending outbox events.
   */
  @Scheduled(fixedDelayString = "${killswitch.outbox.publish-fixed-delay}")
  @Transactional
  public void publishOnce() {
    var pending = outboxRepository.findPending(properties.outbox().maxAttempts());
    if (pending.isEmpty()) {
      return;
    }
    for (var row : pending) {
      try {
        var event = objectMapper.readValue(row.getPayloadJson(), FeatureFlagChangedEvent.class);
        kafkaTemplate.send(properties.kafka().topic(), event.flagName(), event).get();
        row.setPublishedAt(Instant.now());
        row.setLastError(null);
      } catch (Exception e) {
        row.setAttempts(row.getAttempts() + 1);
        row.setLastError(truncate(e.toString(), 1900));
        log.warn("Outbox publish failed for {} (attempt {}): {}", row.getId(), row.getAttempts(), e.toString());
      }
    }
  }

  private static String truncate(String value, int max) {
    if (value == null) {
      return null;
    }
    if (value.length() <= max) {
      return value;
    }
    return value.substring(0, max);
  }
}
