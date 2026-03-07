package com.github.dimitryivaniuta.gateway.killswitch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dimitryivaniuta.gateway.killswitch.cache.FeatureFlagCache;
import com.github.dimitryivaniuta.gateway.killswitch.domain.FeatureFlagAudit;
import com.github.dimitryivaniuta.gateway.killswitch.domain.FeatureFlagState;
import com.github.dimitryivaniuta.gateway.killswitch.domain.OutboxEvent;
import com.github.dimitryivaniuta.gateway.killswitch.messaging.FeatureFlagChangedEvent;
import com.github.dimitryivaniuta.gateway.killswitch.repo.FeatureFlagAuditRepository;
import com.github.dimitryivaniuta.gateway.killswitch.repo.FeatureFlagStateRepository;
import com.github.dimitryivaniuta.gateway.killswitch.repo.OutboxEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin (write) operations for feature flags.
 */
@Service
@RequiredArgsConstructor
public class FeatureFlagAdminService {

  private static final String AGGREGATE_TYPE = "FeatureFlag";
  private static final String EVENT_TYPE = "FeatureFlagChanged";

  private final FeatureFlagStateRepository stateRepository;
  private final FeatureFlagAuditRepository auditRepository;
  private final OutboxEventRepository outboxRepository;
  private final FeatureFlagCache cache;
  private final ObjectMapper objectMapper;

  /**
   * Creates or updates a flag state.
   */
  @Transactional
  public FeatureFlagChangedEvent upsert(String flagName, boolean enabled, String changedBy, String reason) {
    return applyChange(flagName, enabled, changedBy, reason, FeatureFlagAudit.Action.CHANGE);
  }

  /**
   * Rolls a flag back to its previous version.
   */
  @Transactional
  public FeatureFlagChangedEvent rollback(String flagName, String changedBy, String reason) {
    FeatureFlagState current = stateRepository.findById(flagName)
        .orElseThrow(() -> new IllegalArgumentException("Flag does not exist: " + flagName));

    List<FeatureFlagAudit> previous = auditRepository.findPrevious(flagName, current.getVersion());
    if (previous.isEmpty()) {
      throw new IllegalStateException("No previous version found for rollback: " + flagName);
    }
    boolean targetEnabled = previous.getFirst().isEnabled();
    String effectiveReason = (reason == null || reason.isBlank()) ? "rollback" : reason;
    return applyChange(flagName, targetEnabled, changedBy, effectiveReason, FeatureFlagAudit.Action.ROLLBACK);
  }

  private FeatureFlagChangedEvent applyChange(
      String flagName,
      boolean enabled,
      String changedBy,
      String reason,
      FeatureFlagAudit.Action action
  ) {
    Instant now = Instant.now();

    FeatureFlagState state = stateRepository.findById(flagName).orElseGet(() -> {
      FeatureFlagState s = new FeatureFlagState();
      s.setFlagName(flagName);
      s.setVersion(0);
      // defaults for first create
      s.setEnabled(true);
      s.setUpdatedAt(now);
      s.setUpdatedBy("system");
      s.setReason("init");
      return s;
    });

    long newVersion = state.getVersion() + 1;
    state.setEnabled(enabled);
    state.setVersion(newVersion);
    state.setUpdatedAt(now);
    state.setUpdatedBy(changedBy);
    state.setReason(reason);
    stateRepository.save(state);

    FeatureFlagAudit audit = new FeatureFlagAudit();
    audit.setId(UUID.randomUUID());
    audit.setFlagName(flagName);
    audit.setEnabled(enabled);
    audit.setVersion(newVersion);
    audit.setAction(action);
    audit.setChangedAt(now);
    audit.setChangedBy(changedBy);
    audit.setReason(reason);
    auditRepository.save(audit);

    FeatureFlagChangedEvent event = new FeatureFlagChangedEvent(flagName, enabled, newVersion, now, changedBy, reason);
    outboxRepository.save(toOutbox(event, now));

    // Fast local effect: update caches immediately for this node.
    cache.put(event);
    return event;
  }

  private OutboxEvent toOutbox(FeatureFlagChangedEvent event, Instant now) {
    OutboxEvent out = new OutboxEvent();
    out.setId(UUID.randomUUID());
    out.setAggregateType(AGGREGATE_TYPE);
    out.setAggregateId(event.flagName());
    out.setEventType(EVENT_TYPE);
    out.setCreatedAt(now);
    out.setPublishedAt(null);
    out.setAttempts(0);
    out.setLastError(null);
    out.setPayloadJson(writeJson(event));
    return out;
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize payload", e);
    }
  }
}
