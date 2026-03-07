package com.github.dimitryivaniuta.gateway.killswitch.repo;

import com.github.dimitryivaniuta.gateway.killswitch.domain.FeatureFlagAudit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for immutable audit history.
 */
public interface FeatureFlagAuditRepository extends JpaRepository<FeatureFlagAudit, UUID> {

  /** Returns audit history for a flag (most recent first). */
  List<FeatureFlagAudit> findTop50ByFlagNameOrderByVersionDesc(String flagName);

  /** Finds the latest audit entry for a flag. */
  Optional<FeatureFlagAudit> findTop1ByFlagNameOrderByVersionDesc(String flagName);

  /**
   * Finds the previous version audit entry (max version < current).
   */
  @Query("select a from FeatureFlagAudit a where a.flagName = :flag and a.version < :currentVersion order by a.version desc")
  List<FeatureFlagAudit> findPrevious(@Param("flag") String flagName, @Param("currentVersion") long currentVersion);
}
