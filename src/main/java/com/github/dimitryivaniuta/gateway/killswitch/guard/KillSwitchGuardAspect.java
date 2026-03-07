package com.github.dimitryivaniuta.gateway.killswitch.guard;

import com.github.dimitryivaniuta.gateway.killswitch.service.FeatureFlagResolver;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspect that blocks guarded methods when a kill switch flag is disabled.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class KillSwitchGuardAspect {

  private final FeatureFlagResolver resolver;

  /**
   * Checks the flag and blocks execution if disabled.
   */
  @Around("@annotation(guard)")
  public Object guard(ProceedingJoinPoint pjp, KillSwitchGuard guard) throws Throwable {
    boolean enabled = resolver.isEnabled(guard.flag());
    if (!enabled) {
      throw new KillSwitchDisabledException(guard.flag());
    }
    return pjp.proceed();
  }
}
