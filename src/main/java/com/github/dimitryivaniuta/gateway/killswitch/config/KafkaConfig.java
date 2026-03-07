package com.github.dimitryivaniuta.gateway.killswitch.config;

import com.github.dimitryivaniuta.gateway.killswitch.messaging.FeatureFlagChangedEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka wiring ensuring JSON serialization for FeatureFlagChangedEvent.
 */
@Configuration
public class KafkaConfig {

  /**
   * Typed KafkaTemplate for feature flag change events.
   */
  @Bean
  public KafkaTemplate<String, FeatureFlagChangedEvent> featureFlagKafkaTemplate(
      org.springframework.kafka.core.KafkaProperties kafkaProperties
  ) {
    return new KafkaTemplate<>(producerFactory(kafkaProperties));
  }

  private ProducerFactory<String, FeatureFlagChangedEvent> producerFactory(
      org.springframework.kafka.core.KafkaProperties kafkaProperties
  ) {
    Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties(null));
    props.putIfAbsent(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.putIfAbsent(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    // Do not add type headers to keep message stable across stacks.
    props.putIfAbsent(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
    return new DefaultKafkaProducerFactory<>(props);
  }
}
