package de.jpx3.intave.check.movement.physics.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ThreeTickSimulationSearchTest {
  @Test
  void successfulSimulationBreaksFailureStreak() {
    int failures = ThreeTickSimulationSearch.nextHandItemSimulationFailCount(0, true);
    failures = ThreeTickSimulationSearch.nextHandItemSimulationFailCount(failures, true);
    failures = ThreeTickSimulationSearch.nextHandItemSimulationFailCount(failures, false);
    failures = ThreeTickSimulationSearch.nextHandItemSimulationFailCount(failures, true);

    assertEquals(1, failures);
  }

  @Test
  void foodUsageIsNeverForceReset() {
    assertFalse(ThreeTickSimulationSearch.shouldResetItemUsage(3, true));
  }

  @Test
  void nonFoodUsageResetsOnlyAtFailureThreshold() {
    assertFalse(ThreeTickSimulationSearch.shouldResetItemUsage(2, false));
    assertTrue(ThreeTickSimulationSearch.shouldResetItemUsage(3, false));
    assertFalse(ThreeTickSimulationSearch.shouldResetItemUsage(4, false));
  }
}
