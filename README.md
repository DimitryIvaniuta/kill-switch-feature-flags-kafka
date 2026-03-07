# Kill Switch Feature Flags (Kafka + Outbox)

Production-grade reference implementation of a **response kill switch** / **feature flags** mechanism.

## Goals

- **Disable risky features in seconds** across services.
- **Remote config distributed via Kafka**.
- **Audited changes** and **one-click rollback**.
- **Resilient during partial outage** (Kafka/Redis issues):
  - writes are persisted in Postgres and delivered via an **outbox** publisher with retries
  - reads prefer **local cache**, then **Redis**, then **DB** (and fall back to defaults on DB issues)

## Tech

- Java 21, Spring Boot 3.5.10
- PostgreSQL + Flyway
- Kafka (KRaft)
- Redis
- Caffeine local cache
- Testcontainers for integration tests

## Run locally

```bash
docker compose up -d
./gradlew test
./gradlew bootRun
```

Open:

- Risky endpoint: `GET http://localhost:8080/api/risky/quote`
- Admin API:
  - `PUT http://localhost:8080/api/admin/flags/RISKY_RESPONSE`
  - `POST http://localhost:8080/api/admin/flags/RISKY_RESPONSE/rollback`

## Postman

Import: `postman/kill-switch-feature-flags.postman_collection.json`

## How it works

1. Admin changes a flag → state is written to **Postgres** and an audit row is stored.
2. In the same transaction, a JSON payload is stored to **outbox_events**.
3. A scheduled publisher sends outbox events to Kafka and marks them published (with retries).
4. Each service consumes flag change events and updates **local + Redis cache**.
5. Runtime requests use `@KillSwitchGuard(flag = "...")` to short-circuit risky endpoints.

## Configuration

See `src/main/resources/application.yml`.

- `killswitch.cache.local-ttl` — local cache TTL (small)
- `killswitch.cache.redis-ttl` — redis TTL
- `killswitch.kafka.topic` — Kafka topic name
