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

package de.jpx3.intave.executor;

import de.jpx3.intave.math.MathHelper;

import java.util.concurrent.TimeUnit;

public final class RateLimiter {
  private final int max;
  private int counter;
  private long lastReset;
  private final int cooldownPerSecond;

  public RateLimiter(int max, int remove, TimeUnit every) {
    this.max = max;
    this.cooldownPerSecond = (int) (remove / every.toSeconds(1));
    this.lastReset = System.currentTimeMillis();
  }

  public void checkCooldown() {
    long currentTimeMillis = System.currentTimeMillis();
    if (currentTimeMillis - lastReset > 1000L) {
      if (counter > 0) {
        counter -= MathHelper.minmax(0, (int) ((currentTimeMillis - lastReset) / 1000L) * cooldownPerSecond, cooldownPerSecond * 5);
        if (counter < 0) {
          counter = 0;
        }
      }
      lastReset = currentTimeMillis;
    }
  }

  public boolean tryAcquire() {
    checkCooldown();
    if (counter < max) {
      counter++;
      return true;
    }
    return false;
  }

  public void noteAcquired(int amount) {
    checkCooldown();
    counter += amount;
  }

  public boolean isOverLimit() {
    checkCooldown();
    return counter >= max;
  }

  public int maxRequests() {
    return max;
  }

  public int counter() {
    return counter;
  }

  public long lastReset() {
    return lastReset;
  }

  public int cooldownPerSecond() {
    return cooldownPerSecond;
  }
}
