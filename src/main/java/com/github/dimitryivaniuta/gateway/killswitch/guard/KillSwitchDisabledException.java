package com.github.dimitryivaniuta.gateway.killswitch.guard;

/**
 * Raised when an operation is blocked by an active kill switch.
 */
public class KillSwitchDisabledException extends RuntimeException {

  private final String flag;

  /**
   * @param flag flag name
   */
  public KillSwitchDisabledException(String flag) {
    super("Feature disabled by kill switch: " + flag);
    this.flag = flag;
  }

  /** @return flag name */
  public String getFlag() {
    return flag;
  }
}
