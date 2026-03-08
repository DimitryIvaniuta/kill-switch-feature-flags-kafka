package com.github.dimitryivaniuta.gateway.killswitch.web;

import com.github.dimitryivaniuta.gateway.killswitch.domain.FeatureFlagAudit;
import com.github.dimitryivaniuta.gateway.killswitch.messaging.FeatureFlagChangedEvent;
import com.github.dimitryivaniuta.gateway.killswitch.repo.FeatureFlagAuditRepository;
import com.github.dimitryivaniuta.gateway.killswitch.repo.FeatureFlagStateRepository;
import com.github.dimitryivaniuta.gateway.killswitch.service.FeatureFlagAdminService;
import com.github.dimitryivaniuta.gateway.killswitch.service.FeatureFlagResolver;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin API for managing feature flags.
 */
@RestController
@RequestMapping("/api/admin/flags")
@RequiredArgsConstructor
public class AdminFeatureFlagController {

  private final FeatureFlagAdminService adminService;
  private final FeatureFlagResolver resolver;
  private final FeatureFlagStateRepository stateRepository;
  private final FeatureFlagAuditRepository auditRepository;

  /**
   * Read current state.
   */
  @GetMapping("/{flagName}")
  public Map<String, Object> get(@PathVariable String flagName) {
    boolean enabled = resolver.isEnabled(flagName);
    var state = stateRepository.findById(flagName).orElse(null);
    return Map.of(
        "flagName", flagName,
        "enabled", enabled,
        "version", state == null ? 0 : state.getVersion(),
        "updatedAt", state == null ? null : state.getUpdatedAt(),
        "updatedBy", state == null ? null : state.getUpdatedBy(),
        "reason", state == null ? null : state.getReason()
    );
  }

  /**
   * Upsert flag state.
   */
  @PutMapping("/{flagName}")
  public FeatureFlagChangedEvent upsert(@PathVariable String flagName, @Valid @RequestBody UpsertRequest req) {
    return adminService.upsert(flagName, req.enabled(), req.changedBy(), req.reason());
  }

  /**
   * Rollback to previous version.
   */
  @PostMapping("/{flagName}/rollback")
  public FeatureFlagChangedEvent rollback(@PathVariable String flagName, @Valid @RequestBody RollbackRequest req) {
    return adminService.rollback(flagName, req.changedBy(), req.reason());
  }

  /**
   * Returns recent audit history.
   */
  @GetMapping("/{flagName}/audit")
  public List<AuditDto> audit(@PathVariable String flagName) {
    return auditRepository.findTop50ByFlagNameOrderByVersionDesc(flagName).stream()
        .map(AuditDto::from)
        .toList();
  }

  /**
   * Request body for updating a flag.
   */
  public record UpsertRequest(
      @NotNull Boolean enabled,
      @NotBlank String changedBy,
      @NotBlank String reason
  ) {
  }

  /**
   * Request body for rollback.
   */
  public record RollbackRequest(
      @NotBlank String changedBy,
      String reason
  ) {
  }

  /** Audit response DTO. */
  public record AuditDto(
      String flagName,
      boolean enabled,
      long version,
      String action,
      Instant changedAt,
      String changedBy,
      String reason
  ) {
    static AuditDto from(FeatureFlagAudit a) {
      return new AuditDto(
          a.getFlagName(),
          a.isEnabled(),
          a.getVersion(),
          a.getAction().name(),
          a.getChangedAt(),
          a.getChangedBy(),
          a.getReason()
      );
    }
  }
}
