package com.github.dimitryivaniuta.gateway.killswitch.repo;

import com.github.dimitryivaniuta.gateway.killswitch.domain.FeatureFlagState;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for current feature flag state.
 */
public interface FeatureFlagStateRepository extends JpaRepository<FeatureFlagState, String> {
}
