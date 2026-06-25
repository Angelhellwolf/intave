package de.jpx3.intave.check.movement.physics.search;

import de.jpx3.intave.check.movement.physics.Simulator;
import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.user.User;

public final class MovementSearchInput {
  private final User user;
  private final Simulator simulator;
  private final SimulationEnvironment environment;
  private final boolean detectNoSlowdown;

  private MovementSearchInput(User user, Simulator simulator, SimulationEnvironment environment, boolean detectNoSlowdown) {
    this.user = user;
	  this.simulator = simulator;
	  this.environment = environment;
    this.detectNoSlowdown = detectNoSlowdown;
  }

  public static MovementSearchInput from(User user, Simulator simulator, SimulationEnvironment environment, boolean detectNoSlowdown) {
    return new MovementSearchInput(user, simulator, environment, detectNoSlowdown);
  }

  User user() {
    return user;
  }

  Simulator simulator() {
    return simulator;
  }

  SimulationEnvironment environment() {
    return environment.unmodifiable();
  }

  boolean detectNoSlowdown() {
    return detectNoSlowdown;
  }
}
