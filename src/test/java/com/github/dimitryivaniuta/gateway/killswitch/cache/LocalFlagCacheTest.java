package com.github.dimitryivaniuta.gateway.killswitch.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/**
 * Unit test for local TTL cache.
 */
class LocalFlagCacheTest {

  @Test
  void expiresEntry() throws Exception {
    LocalFlagCache cache = new LocalFlagCache();
    CachedFlag f = new CachedFlag("A", true, 1, OffsetDateTime.now(), "u", "r");
    cache.put("A", f, Duration.ofMillis(50));

    assertThat(cache.getIfFresh("A")).isPresent();

    Thread.sleep(80);
    assertThat(cache.getIfFresh("A")).isEmpty();
  }
}
