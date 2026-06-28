package de.jpx3.intave.check.movement.physics.search;

import de.jpx3.intave.check.movement.physics.Simulation;
import de.jpx3.intave.check.movement.physics.Simulator;
import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.user.User;

public interface SimulationSearch {
  Simulation simulate(User user, SimulationEnvironment environment, Simulator simulator);
}
