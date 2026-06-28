package de.jpx3.intave.check.movement.physics.search;

import de.jpx3.intave.check.movement.physics.Simulation;
import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.share.Motion;
import de.jpx3.intave.share.Position;
import de.jpx3.intave.user.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collector;

public final class SimulationCollector {
	private static final int DEFAULT_MAX_FLYING_SIMULATIONS = 8;
	private static final int MOTION_TRACKING_MAX_FLYING_SIMULATIONS = 64;

	private final User user;
	private final SimulationEnvironment environment;
	private final Motion targetMotion;
	private final Position lastReportedPosition;
	private final int maxFlyingSimulations;
	private Simulation best = Simulation.invalid();
	private List<Simulation> possibleFlyingSimulations = null;
	private int totalFlyingPacketSimulations;
	private int simulationsDone;

	private SimulationCollector(
		User user,
		SimulationEnvironment environment,
		int maxFlyingSimulations,
		Motion targetMotion,
		Position lastReportedPosition
	) {
		this.user = user;
		this.environment = environment;
		this.maxFlyingSimulations = maxFlyingSimulations;
		this.targetMotion = targetMotion;
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
		return possibleFlyingSimulations == null ? Collections.emptyList() : possibleFlyingSimulations;
	}

	private SimulationCollector mergedWith(SimulationCollector other) {
		SimulationCollector merged = new SimulationCollector(
			user,
			environment,
			maxFlyingSimulations,
			targetMotion,
			lastReportedPosition
		);
		if (tracksMotion()) {
			merged.add(best);
			merged.add(other.best);
		} else {
			merged.best = selectBest(best, other.best);
		}
		if (possibleFlyingSimulations != null) {
			for (Simulation simulation : possibleFlyingSimulations) {
				merged.add(simulation);
			}
		}
		if (other.possibleFlyingSimulations != null) {
			for (Simulation simulation : other.possibleFlyingSimulations) {
				merged.add(simulation);
			}
		}
		merged.totalFlyingPacketSimulations = totalFlyingPacketSimulations + other.totalFlyingPacketSimulations;
		merged.simulationsDone = simulationsDone + other.simulationsDone;
		return merged;
	}

	private void addFlyingSimulation(Simulation simulation) {
		if (possibleFlyingSimulations == null) {
			possibleFlyingSimulations = new ArrayList<>(Math.min(maxFlyingSimulations, DEFAULT_MAX_FLYING_SIMULATIONS));
		}
		for (Simulation flyingSimulation : possibleFlyingSimulations) {
			if (flyingSimulation.result().almostIdenticalTo(simulation.result())) {
				return;
			}
		}
		if (possibleFlyingSimulations.size() < maxFlyingSimulations) {
			possibleFlyingSimulations.add(simulation.reusableCopy());
		}
	}

	private boolean resultsInFlyingPacket(Simulation simulation) {
		double flyingLimit = user.meta().protocol().flyingPacketUncertaintyRadius();
		if (!tracksMotion()) {
			return simulation.resultsInFlyingPacket(environment, flyingLimit);
		}
		Position simulatedPosition = environment.verifiedLastPosition().add(simulation.motion());
		return lastReportedPosition.distance(simulatedPosition) < flyingLimit;
	}

	private Simulation selectBest(Simulation current, Simulation simulation) {
		if (!tracksMotion()) {
			return current.select(
				simulation,
				environment.position(),
				environment.verifiedLastPosition()
			);
		}
		return current.select(simulation, targetMotion);
	}

	private boolean tracksMotion() {
		return targetMotion != null;
	}

	public static Collector<Simulation, SimulationCollector, SimulationCollector> positionBased(
		User user,
		SimulationEnvironment environment
	) {
		return Collector.of(
			() -> new SimulationCollector(
				user,
				environment,
				DEFAULT_MAX_FLYING_SIMULATIONS,
				null,
				null
			),
			SimulationCollector::add,
			SimulationCollector::mergedWith
		);
	}

	public static Collector<Simulation, SimulationCollector, SimulationCollector> offsetBased(
		User user,
		SimulationEnvironment environment,
		Motion targetMotion,
		Position lastReportedPosition
	) {
		return Collector.of(
			() -> new SimulationCollector(
				user,
				environment,
				MOTION_TRACKING_MAX_FLYING_SIMULATIONS,
				targetMotion,
				lastReportedPosition
			),
			SimulationCollector::add,
			SimulationCollector::mergedWith
		);
	}
}
