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
import de.jpx3.intave.user.meta.MovementMetadata;

import java.util.HashSet;
import java.util.Set;

public final class RollbackSimulationSearch implements SimulationSearch {
	private final SimulationSearch delegate;
	private final SimulationEvaluator evaluator;

	private RollbackSimulationSearch(SimulationSearch delegate, SimulationEvaluator evaluator) {
		this.delegate = delegate;
		this.evaluator = evaluator;
	}

	@Override
	public Set<Simulation> exhaustiveSearch(User user, SimulationEnvironment environment, Simulator simulator) {
		return delegate.exhaustiveSearch(user, environment, simulator);
	}

	@Override
	public Simulation search(
		User user, SimulationEnvironment currentEnvironment,
		Simulator simulator, SimulationSearchOptions options
	) {
		Simulation firstSimulation = delegate.search(user, currentEnvironment, simulator, options);
		MovementMetadata movement = user.meta().movement();
		SimulationEnvironment rollbackEnvironment = movement.beforePreviousTickEnvironment;

		if (firstSimulation.offsetDifference() < 0.0005 || evaluate(user, firstSimulation) < 0.01 || rollbackEnvironment == null) {
			storeRollbackEnvironment(movement, firstSimulation);
			return firstSimulation;
		}

		Set<Simulation> simulations = delegate.exhaustiveSearch(user, rollbackEnvironment, simulator);
		if (simulations.size() <= 1) {
			storeRollbackEnvironment(movement, firstSimulation);
			return firstSimulation;
		}

		Simulation bestSimulation = firstSimulation;
		Set<Motion> bestOffsetMotions = new HashSet<>();
		for (Simulation simulation : simulations) {
			if (!simulation.offsetMotionDiffersFromActualMotion()) {
				continue;
			}
			SimulationEnvironment firstTickEnvironment = simulation.environment().mutableView();
			simulator.simulateAround(
				user, firstTickEnvironment, simulation,
				currentEnvironment.position(), currentEnvironment.rotation()
			);
			Motion outputMotion = firstTickEnvironment.mutableBaseMotionCopy();
			if (bestOffsetMotions.contains(outputMotion)) {
				continue;
			}
			bestOffsetMotions.add(outputMotion);
			Simulation nextSimulation = delegate.search(
				user, firstTickEnvironment, simulator, options
			);
			bestSimulation = nextSimulation.select(bestSimulation);
			if (bestSimulation.offsetDifference() < 0.0005) {
				storeRollbackEnvironment(movement, bestSimulation);
				return bestSimulation;
			}
		}
		if (bestOffsetMotions.size() == 1) {
			storeRollbackEnvironment(movement, firstSimulation);
			return firstSimulation;
		}
		if (!bestOffsetMotions.isEmpty()) {
			user.player().sendMessage("Rollbacked ("+bestOffsetMotions.size()+") simulation search: " + bestSimulation.offsetDifference() + " vs " + firstSimulation.offsetDifference());
		}
		storeRollbackEnvironment(movement, bestSimulation);
		return bestSimulation;
	}

	private void storeRollbackEnvironment(MovementMetadata movement, Simulation simulation) {
		movement.beforePreviousTickEnvironment = simulation.environment().immutableCopy();
	}

	private double evaluate(
		User user, Simulation simulation
	) {
		Motion offsetMotion = simulation.offsetMotion();
		Set<EvaluationTag> unusedEvalTags = new HashSet<>();
		double horizontalVL = evaluator.calculateHorizontalViolationIncrease(
			user, simulation.environment(), offsetMotion.motionX, offsetMotion.motionZ,
			false, false, unusedEvalTags
		);
		double verticalVL = evaluator.calculateVerticalViolationLevelIncrease(
			user, simulation.environment(), offsetMotion.motionY,
			false, false, unusedEvalTags
		);
		return horizontalVL + verticalVL;
	}

	public static RollbackSimulationSearch of(SimulationSearch delegate, SimulationEvaluator evaluator) {
		return new RollbackSimulationSearch(delegate, evaluator);
	}
}
