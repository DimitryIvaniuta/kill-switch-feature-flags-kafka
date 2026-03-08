package com.github.dimitryivaniuta.gateway.killswitch;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dimitryivaniuta.gateway.killswitch.repo.FeatureFlagOutboxRepository;
import com.github.dimitryivaniuta.gateway.killswitch.web.dto.SetFlagRequest;
import java.time.Duration;
import java.util.Map;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * End-to-end integration test with Postgres + Kafka + Redis using Testcontainers.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FeatureFlagIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("killswitch")
      .withUsername("killswitch")
      .withPassword("killswitch");

  @Container
  static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

  @Container
  static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
      .withExposedPorts(6379);

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    r.add("spring.datasource.username", POSTGRES::getUsername);
    r.add("spring.datasource.password", POSTGRES::getPassword);
    r.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    r.add("spring.data.redis.host", REDIS::getHost);
    r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    r.add("killswitch.cache.local-ttl", () -> "100ms");
    r.add("killswitch.cache.redis-ttl", () -> "1s");
    r.add("killswitch.outbox.publish-fixed-delay", () -> "200ms");
  }

  @LocalServerPort
  int port;

  @Autowired
  TestRestTemplate http;

  @Autowired
  FeatureFlagOutboxRepository outbox;

  @Test
  void killSwitchDisablesRiskyEndpointQuickly_andOutboxPublishesToKafka() {
    String base = "http://localhost:" + port;

    // Initially allowed (default enabled-if-missing=true)
    ResponseEntity<Map> ok = http.getForEntity(base + "/api/risky/quote", Map.class);
    assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Disable the endpoint
    SetFlagRequest req = new SetFlagRequest(false, "tester", "incident response");
    ResponseEntity<Map> set = http.postForEntity(base + "/api/admin/flags/RISKY_RESPONSE", req, Map.class);
    assertThat(set.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Must be disabled in seconds (here: immediately on same node)
    ResponseEntity<Map> blocked = http.getForEntity(base + "/api/risky/quote", Map.class);
    assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

    // Outbox should be published shortly (Kafka + scheduled publisher)
    Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      long published = outbox.findAll().stream().filter(e -> e.getPublishedAt() != null).count();
      assertThat(published).isGreaterThanOrEqualTo(1);
    });
  }
}
