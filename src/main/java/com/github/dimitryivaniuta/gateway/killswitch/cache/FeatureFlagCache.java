package com.github.dimitryivaniuta.gateway.killswitch.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.dimitryivaniuta.gateway.killswitch.config.KillSwitchProperties;
import com.github.dimitryivaniuta.gateway.killswitch.messaging.FeatureFlagChangedEvent;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Multi-layer cache:
 * <ol>
 *   <li>Local (Caffeine) cache</li>
 *   <li>Redis cache (shared across instances)</li>
 * </ol>
 *
 * The DB remains the source of truth.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagCache {

  private final Cache<String, FeatureFlagChangedEvent> localCache;
  private final RedisTemplate<String, FeatureFlagChangedEvent> redisTemplate;
  private final KillSwitchProperties properties;

  /**
   * Try to read a flag snapshot from cache.
   */
  public Optional<FeatureFlagChangedEvent> get(String flagName) {
    var fromLocal = Optional.ofNullable(localCache.getIfPresent(flagName));
    if (fromLocal.isPresent()) {
      return fromLocal;
    }

    try {
      var fromRedis = redisTemplate.opsForValue().get(redisKey(flagName));
      if (fromRedis != null) {
        localCache.put(flagName, fromRedis);
        return Optional.of(fromRedis);
      }
    } catch (Exception e) {
      // Partial outage: Redis might be unavailable. Keep working from local cache.
      log.warn("Redis read failed for flag {}: {}", flagName, e.toString());
    }
    return Optional.empty();
  }

  /**
   * Put a snapshot to caches.
   */
  public void put(FeatureFlagChangedEvent event) {
    if (event == null || event.flagName() == null) {
      return;
    }
    localCache.put(event.flagName(), event);
    try {
      Duration ttl = properties.cache().redisTtl();
      redisTemplate.opsForValue().set(redisKey(event.flagName()), event, ttl);
    } catch (Exception e) {
      log.warn("Redis write failed for flag {}: {}", event.flagName(), e.toString());
    }
  }

  private static String redisKey(String flagName) {
    return "killswitch:flag:" + flagName;
  }
}
