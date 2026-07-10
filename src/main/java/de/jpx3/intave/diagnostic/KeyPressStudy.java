/*
 * Copyright 2026 Intave
 *
 * This software is licensed under the PolyForm Perimeter License 1.0.0.
 * You may use this software for any purpose, except for providing to
 * others any product that competes with the software.
 *
 * A copy of the license is available at:
 *   https://polyformproject.org/licenses/perimeter/1.0.0/
 */

package de.jpx3.intave.diagnostic;

import de.jpx3.intave.check.movement.physics.config.MovementConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class KeyPressStudy {
  private static final Map<String, Long> keys = new ConcurrentHashMap<>();

  public static void enterKeyPress(int forward, int strafe) {
    String keys = resolveKeysFromInput(forward, strafe);
    KeyPressStudy.keys.put(keys, KeyPressStudy.keys.getOrDefault(keys, 0L) + 1);
  }

  public static void enterKeyPressFrom(MovementConfiguration configuration) {
    enterKeyPress(
      configuration.forward(),
      configuration.strafe()
    );
  }

  public static Map<String, Long> result() {
    return keys;
  }

  public static Map<String, Double> resultShare() {
    Map<String, Double> shareMap = new HashMap<>();
    long total = keys.values().stream().mapToLong(l -> l).sum();
    keys.forEach((key, value) -> shareMap.put(key, (double) value / (double) total));
    return shareMap;
  }

  private static String resolveKeysFromInput(int forward, int strafe) {
    String key = "";
    if (forward == 1) {
      key += "W";
    } else if (forward == -1) {
      key += "S";
    }
    if (strafe == 1) {
      key += "A";
    } else if (strafe == -1) {
      key += "D";
    }
    return key;
  }
}
