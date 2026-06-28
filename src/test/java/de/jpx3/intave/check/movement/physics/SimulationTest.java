package de.jpx3.intave.check.movement.physics;

import de.jpx3.intave.check.movement.physics.environment.TestSimulationEnvironment;
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

    simulation.append("1f");

    assertEquals("1f", simulation.reusableCopy().details());
  }

  @Test
  void flushClearsDetails() {
    Simulation simulation = simulation();
    simulation.append("1f");

    simulation.flush(
      MovementConfiguration.blank(),
      new TestSimulationEnvironment(),
      SimulationResult.untouched(Motion.newEmpty())
    );

    assertEquals("", simulation.details());
  }

  private Simulation simulation() {
    return Simulation.of(
      userWithoutPlayer(),
      MovementConfiguration.blank(),
      new TestSimulationEnvironment(),
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
