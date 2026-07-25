package de.jpx3.intave.check.world.placementanalysis;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Regression coverage for the semantic five-second activity window and the
// independent defensive 100-sample memory bound.
class RotationSpeedTest {
  @Test
  void ignoresSamplesOutsideTheRotationWindow() {
    List<RotationSpeed.RotationSample> history = new ArrayList<>();
    RotationSpeed.recordRotation(history, 100.0F, 0);
    RotationSpeed.recordRotation(history, 200.0F, 4999);

    assertEquals(200.0, RotationSpeed.rotationSum(history, 5001));
    assertEquals(1, history.size());
  }

  @Test
  void keepsSamplesOnTheWindowBoundary() {
    List<RotationSpeed.RotationSample> history = new ArrayList<>();
    RotationSpeed.recordRotation(history, 100.0F, 0);

    // Samples exactly on the cutoff remain part of the active window.
    assertEquals(100.0, RotationSpeed.rotationSum(history, 5000));
    assertEquals(1, history.size());
  }

  @Test
  void capsTheNumberOfRotationSamples() {
    List<RotationSpeed.RotationSample> history = new ArrayList<>();
    for (int i = 0; i < 101; i++) {
      RotationSpeed.recordRotation(history, 1.0F, i);
    }

    assertEquals(100, history.size());
    assertEquals(100.0, RotationSpeed.rotationSum(history, 101));
  }
}
