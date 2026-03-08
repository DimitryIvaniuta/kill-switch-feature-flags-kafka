package com.github.dimitryivaniuta.gateway.killswitch;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class that starts Postgres + Kafka + Redis via Testcontainers.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractIntegrationTest {

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

  @BeforeAll
  void startContainers() {
    // Ensure containers are started before property registration.
    POSTGRES.start();
    KAFKA.start();
    REDIS.start();
  }

  @DynamicPropertySource
  static void registerProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);

    registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

    registry.add("killswitch.kafka.topic", () -> "feature-flags.v1");
    registry.add("killswitch.cache.local-ttl", () -> "200ms");
    registry.add("killswitch.cache.redis-ttl", () -> "500ms");
    registry.add("killswitch.outbox.publish-fixed-delay", () -> "200ms");
  }
}
