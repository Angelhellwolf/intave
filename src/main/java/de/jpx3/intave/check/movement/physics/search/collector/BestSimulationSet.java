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

package de.jpx3.intave.check.movement.physics.search.collector;

import de.jpx3.intave.check.movement.physics.simulator.Simulation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class BestSimulationSet {
	private final Set<Simulation> simulations = new HashSet<>();
	private double bestDistance = Double.MAX_VALUE;
	private boolean bestCanFinishExplicitTick;

	void add(Simulation simulation, double distance) {
		boolean canFinishExplicitTick = simulation.canFinishExplicitTick();
		if (simulations.isEmpty()
			|| (canFinishExplicitTick && !bestCanFinishExplicitTick)
			|| (canFinishExplicitTick == bestCanFinishExplicitTick && (distance < bestDistance) )
		) {
			simulations.clear();
			bestDistance = distance;
			bestCanFinishExplicitTick = canFinishExplicitTick;
			simulations.add(simulation.reusableCopy());
		} else if (canFinishExplicitTick == bestCanFinishExplicitTick && Math.abs(distance - bestDistance) < 1e-6) {
			simulations.add(simulation.reusableCopy());
		}
	}

	public Set<Simulation> simulations() {
		return Collections.unmodifiableSet(simulations);
	}
}
