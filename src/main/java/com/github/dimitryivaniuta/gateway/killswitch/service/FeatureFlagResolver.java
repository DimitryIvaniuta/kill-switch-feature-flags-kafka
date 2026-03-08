package com.github.dimitryivaniuta.gateway.killswitch.service;

import com.github.dimitryivaniuta.gateway.killswitch.cache.FeatureFlagCache;
import com.github.dimitryivaniuta.gateway.killswitch.config.KillSwitchProperties;
import com.github.dimitryivaniuta.gateway.killswitch.messaging.FeatureFlagChangedEvent;
import com.github.dimitryivaniuta.gateway.killswitch.repo.FeatureFlagStateRepository;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Read-only resolver used in hot request path.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagResolver {

  private final FeatureFlagCache cache;
  private final FeatureFlagStateRepository stateRepository;
  private final KillSwitchProperties properties;

  /**
   * Returns whether a flag is enabled.
   *
   * Lookup order: local cache -> Redis -> DB.
   *
   * During partial outages (DB/Redis unavailable), falls back to cached values and finally
   * to {@code killswitch.defaults.enabled-if-missing}.
   */
  public boolean isEnabled(String flagName) {
    Optional<FeatureFlagChangedEvent> cached = cache.get(flagName);
    if (cached.isPresent()) {
      return cached.get().enabled();
    }
    try {
      return stateRepository.findById(flagName)
          .map(s -> {
            var ev = new FeatureFlagChangedEvent(
                s.getFlagName(),
                s.isEnabled(),
                s.getVersion(),
                s.getUpdatedAt(),
                s.getUpdatedBy(),
                s.getReason());
            cache.put(ev);
            return ev.enabled();
          })
          .orElseGet(() -> {
            // Store a synthetic snapshot to avoid hammering DB for missing keys.
            boolean defaultEnabled = properties.defaults().enabledIfMissing();
            cache.put(new FeatureFlagChangedEvent(
                flagName,
                defaultEnabled,
                0,
                Instant.now(),
                "system",
                "default"));
            return defaultEnabled;
          });
    } catch (Exception e) {
      log.warn("DB read failed for flag {}: {}", flagName, e.toString());
      return properties.defaults().enabledIfMissing();
    }
  }
}
