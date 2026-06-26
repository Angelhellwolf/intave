package de.jpx3.intave.check.movement.physics;

import de.jpx3.intave.player.collider.complex.SimulationResult;
import de.jpx3.intave.share.Motion;
import de.jpx3.intave.user.User;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SimulationTest {
  @Test
  void reusableCopyPreservesDetails() {
    Simulation simulation = simulation();

    simulation.append("2t");

    assertEquals("2t", simulation.reusableCopy().details());
  }

  @Test
  void flushClearsDetails() {
    Simulation simulation = simulation();
    simulation.append("2t");

    simulation.flush(MovementConfiguration.blank(), SimulationResult.untouched(Motion.newEmpty()));

    assertEquals("", simulation.details());
  }

  private Simulation simulation() {
    return Simulation.of(
      userWithoutPlayer(),
      MovementConfiguration.blank(),
      SimulationResult.untouched(Motion.newEmpty())
    );
  }

  private User userWithoutPlayer() {
    return (User) Proxy.newProxyInstance(
      User.class.getClassLoader(),
      new Class<?>[]{User.class},
      (proxy, method, args) -> {
        if ("hasPlayer".equals(method.getName())) {
          return false;
        }
        return null;
      }
    );
  }
}
