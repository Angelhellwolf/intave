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
import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.share.Motion;
import de.jpx3.intave.share.Position;
import de.jpx3.intave.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collector;

public final class SimulationCollector {
	private static final int DEFAULT_MAX_FLYING_SIMULATIONS = 4;

	private final User user;
	private final SimulationEnvironment environment;
	private final Motion targetOffsetMotion;
	private final Position lastReportedPosition;
	private final int maxFlyingSimulations;
	private Simulation best = Simulation.invalid();
	private Map<Long, Simulation> possibleFlyingSimulationsByHash = null;
	private int totalFlyingPacketSimulations;
	private int simulationsDone;

	private SimulationCollector(
		User user,
		SimulationEnvironment environment,
		int maxFlyingSimulations,
		Motion targetOffsetMotion,
		Position lastReportedPosition
	) {
		this.user = user;
		this.environment = environment;
		this.maxFlyingSimulations = maxFlyingSimulations;
		this.targetOffsetMotion = targetOffsetMotion;
		this.lastReportedPosition = lastReportedPosition;
	}

	private void add(Simulation simulation) {
		if (resultsInFlyingPacket(simulation)) {
			addFlyingSimulation(simulation);
			totalFlyingPacketSimulations++;
		}
		best = selectBest(best, simulation);
		simulationsDone++;
	}

	public int flyingPacketSimulations() {
		return totalFlyingPacketSimulations;
	}

	public int simulationsDone() {
		return simulationsDone;
	}

	public Simulation bestSimulation() {
		return best;
	}

	public List<Simulation> flyingSimulations() {
		return possibleFlyingSimulationsByHash == null ? Collections.emptyList() : new ArrayList<>(possibleFlyingSimulationsByHash.values());
	}

	private SimulationCollector mergedWith(SimulationCollector other) {
		SimulationCollector merged = new SimulationCollector(
			user,
			environment,
			maxFlyingSimulations,
			targetOffsetMotion,
			lastReportedPosition
		);
		merged.add(best);
		merged.add(other.best);

		if (possibleFlyingSimulationsByHash != null) {
			possibleFlyingSimulationsByHash.values().forEach(merged::add);
		}
		if (other.possibleFlyingSimulationsByHash != null) {
			other.possibleFlyingSimulationsByHash.values().forEach(merged::add);
		}
		merged.totalFlyingPacketSimulations = totalFlyingPacketSimulations + other.totalFlyingPacketSimulations;
		merged.simulationsDone = simulationsDone + other.simulationsDone;
		return merged;
	}

	private void addFlyingSimulation(Simulation simulation) {
		if (possibleFlyingSimulationsByHash == null) {
			possibleFlyingSimulationsByHash = new HashMap<>();
		}
		long almostIdenticalHash = simulation.result().almostIdenticalHash();
		Simulation almostIdenticalSimulation;
		do {
			almostIdenticalSimulation = possibleFlyingSimulationsByHash.get(almostIdenticalHash);
			almostIdenticalHash++;
		} while (almostIdenticalSimulation != null && !almostIdenticalSimulation.result().almostIdenticalTo(simulation.result()));
		possibleFlyingSimulationsByHash.put(--almostIdenticalHash, simulation.reusableCopy());

		if (possibleFlyingSimulationsByHash.size() > maxFlyingSimulations) {
			Long worstKey = null;
			Simulation worstSim = null;

			for (Map.Entry<Long, Simulation> entry : possibleFlyingSimulationsByHash.entrySet()) {
				if (worstSim == null) {
					worstKey = entry.getKey();
					worstSim = entry.getValue();
				} else {
					Simulation best = selectBestFlying(worstSim, entry.getValue(), simulation.environment().position());
					if (best == worstSim) {
						worstKey = entry.getKey();
						worstSim = entry.getValue();
					}
				}
			}
			if (worstKey != null) {
				possibleFlyingSimulationsByHash.remove(worstKey);
			}
		}
	}

	private boolean resultsInFlyingPacket(Simulation simulation) {
		double flyingLimit = user.meta().protocol().flyingPacketUncertaintyRadius();
		Position simulatedPosition = environment.verifiedLastPosition().add(simulation.offsetMotion());
		return lastReportedPosition.distance(simulatedPosition) < flyingLimit;
	}

	private Simulation selectBestFlying(Simulation a, Simulation b, Position receivedPosition) {
		if (a == Simulation.invalid()) {
			return b;
		}
		if (b == Simulation.invalid()) {
			return a;
		}
		Position positionA = a.environment().lastPosition().add(a.result().offsetMotion());
		Position positionB = b.environment().lastPosition().add(b.result().offsetMotion());
		double distanceA = receivedPosition.distance(positionA);
		double distanceB = receivedPosition.distance(positionB);
		if (distanceA < distanceB) {
			return a;
		} {
			return b;
		}
	}

	private Simulation selectBest(Simulation current, Simulation simulation) {
		return current.select(simulation, targetOffsetMotion);
	}

	public static Collector<Simulation, SimulationCollector, SimulationCollector> forEnvironment(
		User user, SimulationEnvironment environment, int maxFlyingSimulations
	) {
		return forEnvironmentWithCustomTargets(
			user, environment, environment.sentOffsetMotion(), environment.lastPosition(), maxFlyingSimulations
		);
	}

	public static Collector<Simulation, SimulationCollector, SimulationCollector> forEnvironmentWithCustomTargets(
		User user, SimulationEnvironment environment,
		@NotNull Motion targetOffset,
		@NotNull Position lastReportedPosition,
		int maxFlyingSimulations
	) {
		return Collector.of(
			() -> new SimulationCollector(
				user,
				environment,
				maxFlyingSimulations,
				targetOffset,
				lastReportedPosition
			),
			SimulationCollector::add,
			SimulationCollector::mergedWith
		);
	}
}
