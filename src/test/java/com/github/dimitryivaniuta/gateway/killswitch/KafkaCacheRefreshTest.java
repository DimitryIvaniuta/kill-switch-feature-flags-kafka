package com.github.dimitryivaniuta.gateway.killswitch;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dimitryivaniuta.gateway.killswitch.config.KillSwitchProperties;
import com.github.dimitryivaniuta.gateway.killswitch.messaging.FeatureFlagChangedEvent;
import com.github.dimitryivaniuta.gateway.killswitch.service.FeatureFlagResolver;
import java.time.Instant;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Verifies that Kafka events refresh caches.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class KafkaCacheRefreshTest extends AbstractIntegrationTest {

  @Autowired
  private KafkaTemplate<String, FeatureFlagChangedEvent> kafkaTemplate;

  @Autowired
  private KillSwitchProperties properties;

  @Autowired
  private FeatureFlagResolver resolver;

  @Test
  void kafkaEventRefreshesResolver() {
    String flag = "KAFKA_ONLY_FLAG";
    // default is enabled if missing
    assertThat(resolver.isEnabled(flag)).isTrue();

    kafkaTemplate.send(properties.kafka().topic(), flag,
        new FeatureFlagChangedEvent(flag, false, 1, Instant.now(), "test", "kafka"));

    Awaitility.await().untilAsserted(() -> assertThat(resolver.isEnabled(flag)).isFalse());
  }
}
