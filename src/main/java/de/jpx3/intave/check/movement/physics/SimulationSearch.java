package de.jpx3.intave.check.movement.physics;

import de.jpx3.intave.user.User;

public interface SimulationSearch {
  Simulation simulate(User user, Simulator simulator);
}
