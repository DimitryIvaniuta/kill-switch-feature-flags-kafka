package com.github.dimitryivaniuta.gateway.killswitch.web;

import com.github.dimitryivaniuta.gateway.killswitch.guard.KillSwitchGuard;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example endpoint representing a potentially risky response.
 */
@RestController
@RequestMapping("/api/risky")
public class RiskyController {

  /**
   * Example risky endpoint that can be disabled instantly.
   */
  @GetMapping("/quote")
  @KillSwitchGuard(flag = "RISKY_RESPONSE")
  public Map<String, Object> riskyQuote() {
    return Map.of(
        "ts", Instant.now().toString(),
        "quote", "Ship fast, rollback faster."
    );
  }
}
