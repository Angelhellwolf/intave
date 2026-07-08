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

package de.jpx3.intave.check.movement.physics.search;

import de.jpx3.intave.check.movement.physics.Simulation;
import de.jpx3.intave.check.movement.physics.Simulator;
import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.user.User;

import java.util.Set;

public interface SimulationSearch {
  default Simulation greedyFuzzySearch(User user, SimulationEnvironment environment, Simulator simulator) {
    return search(user, environment, simulator, SimulationSearchOptions.GREEDY_FUZZY);
  }

  default Simulation greedyFullSearch(User user, SimulationEnvironment environment, Simulator simulator) {
    return search(user, environment, simulator, SimulationSearchOptions.GREEDY_EXACT);
  }

	Set<Simulation> exhaustiveSearch(User user, SimulationEnvironment environment, Simulator simulator);

  Simulation search(
	  User user, SimulationEnvironment movementData,
	  Simulator simulator, SimulationSearchOptions options
  );
}
