package com.github.dimitryivaniuta.gateway.killswitch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.dimitryivaniuta.gateway.killswitch.messaging.FeatureFlagChangedEvent;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

/**
 * Application-level wiring for the kill switch feature flag subsystem.
 */
@Configuration
@EnableConfigurationProperties(KillSwitchProperties.class)
public class AppConfig {

  /**
   * Local in-memory cache for feature flags.
   */
  @Bean
  public Cache<String, FeatureFlagChangedEvent> localFlagCache(KillSwitchProperties properties) {
    Duration ttl = properties.cache().localTtl();
    return Caffeine.newBuilder()
        .expireAfterWrite(ttl)
        .maximumSize(10_000)
        .build();
  }

  /**
   * RedisTemplate used to cache flag snapshots.
   */
  @Bean
  public RedisTemplate<String, FeatureFlagChangedEvent> flagRedisTemplate(
      RedisConnectionFactory connectionFactory,
      ObjectMapper objectMapper
  ) {
    var template = new RedisTemplate<String, FeatureFlagChangedEvent>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    var serializer = new Jackson2JsonRedisSerializer<>(FeatureFlagChangedEvent.class);
    serializer.setObjectMapper(objectMapper);
    template.setValueSerializer(serializer);
    template.setHashValueSerializer(serializer);
    template.afterPropertiesSet();
    return template;
  }

  /**
   * Configure JsonDeserializer to not require type headers.
   */
  @Bean
  public JsonDeserializer<FeatureFlagChangedEvent> featureFlagChangedEventJsonDeserializer() {
    var deserializer = new JsonDeserializer<>(FeatureFlagChangedEvent.class);
    deserializer.addTrustedPackages("com.github.dimitryivaniuta.gateway.killswitch.messaging");
    deserializer.ignoreTypeHeaders();
    return deserializer;
  }
}
