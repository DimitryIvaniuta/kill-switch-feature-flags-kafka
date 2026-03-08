package com.github.dimitryivaniuta.gateway.killswitch;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dimitryivaniuta.gateway.killswitch.web.AdminFeatureFlagController.UpsertRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * End-to-end verification:
 * - disable flag
 * - risky endpoint becomes unavailable (503)
 * - enable flag
 * - risky endpoint works again
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KillSwitchE2ETest extends AbstractIntegrationTest {

  @Autowired
  private TestRestTemplate rest;

  @Test
  void killSwitchDisablesRiskyEndpointInSeconds() {
    // enabled by default if missing
    ResponseEntity<String> initial = rest.getForEntity("/api/risky/quote", String.class);
    assertThat(initial.getStatusCode()).isEqualTo(HttpStatus.OK);

    var disable = new UpsertRequest(false, "test", "incident");
    ResponseEntity<String> changed = rest.exchange(
        "/api/admin/flags/RISKY_RESPONSE",
        org.springframework.http.HttpMethod.PUT,
        new HttpEntity<>(disable),
        String.class
    );
    assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<String> blocked = rest.getForEntity("/api/risky/quote", String.class);
    assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

    var enable = new UpsertRequest(true, "test", "resolved");
    ResponseEntity<String> enabled = rest.exchange(
        "/api/admin/flags/RISKY_RESPONSE",
        org.springframework.http.HttpMethod.PUT,
        new HttpEntity<>(enable),
        String.class
    );
    assertThat(enabled.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<String> okAgain = rest.getForEntity("/api/risky/quote", String.class);
    assertThat(okAgain.getStatusCode()).isEqualTo(HttpStatus.OK);
  }
}
