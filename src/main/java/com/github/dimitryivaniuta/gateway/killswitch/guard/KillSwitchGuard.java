package com.github.dimitryivaniuta.gateway.killswitch.guard;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint or method as guarded by a kill switch flag.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface KillSwitchGuard {

  /**
   * Flag name.
   */
  String flag();
}
