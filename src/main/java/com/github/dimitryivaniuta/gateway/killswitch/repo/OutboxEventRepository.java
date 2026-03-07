package com.github.dimitryivaniuta.gateway.killswitch.repo;

import com.github.dimitryivaniuta.gateway.killswitch.domain.OutboxEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for transactional outbox events.
 */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

  /**
   * Fetch a batch of pending (not published) events under max attempts.
   */
  @Query("select e from OutboxEvent e where e.publishedAt is null and e.attempts < :maxAttempts order by e.createdAt asc")
  List<OutboxEvent> findPending(@Param("maxAttempts") int maxAttempts);

  /**
   * Fetch events that failed permanently (attempts >= maxAttempts).
   */
  @Query("select e from OutboxEvent e where e.publishedAt is null and e.attempts >= :maxAttempts and e.createdAt < :olderThan")
  List<OutboxEvent> findDeadLetters(@Param("maxAttempts") int maxAttempts, @Param("olderThan") Instant olderThan);
}
