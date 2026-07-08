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

import de.jpx3.intave.check.movement.physics.EvaluationTag;
import de.jpx3.intave.check.movement.physics.Simulation;
import de.jpx3.intave.check.movement.physics.SimulationEvaluator;
import de.jpx3.intave.check.movement.physics.Simulator;
import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.share.Motion;
import de.jpx3.intave.user.User;

import java.util.HashSet;
import java.util.Set;

public final class RedoSimulationSearch implements SimulationSearch {
	private final SimulationSearch delegate;
	private final SimulationEvaluator evaluator;

	public RedoSimulationSearch(SimulationSearch delegate, SimulationEvaluator evaluator) {
		this.delegate = delegate;
		this.evaluator = evaluator;
	}

	@Override
	public Set<Simulation> exhaustiveSearch(User user, SimulationEnvironment environment, Simulator simulator) {
		return delegate.exhaustiveSearch(user, environment, simulator);
	}

	@Override
	public Simulation search(
		User user, SimulationEnvironment simulationEnvironment,
		Simulator simulator, SimulationSearchOptions options
	) {
		if (!options.allowFuzziness()) {
			return delegate.search(user, simulationEnvironment, simulator, options);
		}
		SimulationEnvironment branchEnvironment = simulationEnvironment.mutableView();
		Simulation firstSimulation = delegate.greedyFuzzySearch(user, branchEnvironment, simulator);

		double difference = firstSimulation.offsetDifference();
		if (difference < 0.0005 || firstSimulation.isFromExhaustiveSearch()) {
			return firstSimulation;
		}

		Motion offsetMotion = firstSimulation.offsetMotion();
		SimulationEnvironment resultEnvironment = firstSimulation.environment();
		Set<EvaluationTag> unusedEvalTags = new HashSet<>();
		double horizontalVL = evaluator.calculateHorizontalViolationIncrease(
			user, resultEnvironment, offsetMotion.motionX, offsetMotion.motionZ, false, false, unusedEvalTags
		);
		if (horizontalVL > 0) {
			Simulation simulation = delegate.greedyFullSearch(user, simulationEnvironment, simulator);
			simulation.appendPurple("redo:H("+firstSimulation.blueDetails()+")");
			return simulation;
		}
		double verticalVL = evaluator.calculateVerticalViolationLevelIncrease(
			user, resultEnvironment, offsetMotion.motionY, false, false, unusedEvalTags
		);
		if (verticalVL > 0) {
			Simulation simulation = delegate.greedyFullSearch(user, simulationEnvironment, simulator);
			simulation.appendPurple("redo:V("+firstSimulation.blueDetails()+")");
			return simulation;
		}
		return firstSimulation;
	}

	public static SimulationSearch of(SimulationSearch search, SimulationEvaluator simulationEvaluator) {
		return new RedoSimulationSearch(search, simulationEvaluator);
	}
}
