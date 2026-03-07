package com.github.dimitryivaniuta.gateway.killswitch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Kill Switch Feature Flags application.
 */
@SpringBootApplication
@EnableScheduling
public class KillSwitchApplication {

  /**
   * Boots the Spring application.
   *
   * @param args CLI args
   */
  public static void main(String[] args) {
    SpringApplication.run(KillSwitchApplication.class, args);
  }
}
