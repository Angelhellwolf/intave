package de.jpx3.intave.module.dispatch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MovementDispatcherTest {
  @Test
  void preservesRecentServerVelocityAfterDismount() {
    assertFalse(MovementDispatcher.shouldResetDismountMotion(2.8, 0, 0));
    assertFalse(MovementDispatcher.shouldResetDismountMotion(2.8, 1, 1));
  }

  @Test
  void resetsUnexplainedLargeDismountMotion() {
    assertTrue(MovementDispatcher.shouldResetDismountMotion(0.6, 0, 2));
  }
}
