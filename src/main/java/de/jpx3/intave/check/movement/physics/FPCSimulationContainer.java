package de.jpx3.intave.check.movement.physics;

import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.user.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FPCSimulationContainer {
	private Simulation best = Simulation.invalid();
	private List<Simulation> possibleFlyingSimulations = null;
	private final int maxFlyingSimulations;

	public FPCSimulationContainer(int maxFlyingSimulations) {
		this.maxFlyingSimulations = maxFlyingSimulations;
	}

	public void add(Simulation simulation, User user, SimulationEnvironment environment) {
		double flyingLimit = user.meta().protocol().flyingPacketUncertaintyRadius();

		if (simulation.resultsInFlyingPacket(environment, flyingLimit)) {
			if (possibleFlyingSimulations == null) {
				possibleFlyingSimulations = new ArrayList<>(maxFlyingSimulations);
			}
			boolean contained = false;
			for (Simulation flyingSimulation : possibleFlyingSimulations) {
				if (flyingSimulation.result().almostIdenticalTo(simulation.result())) {
					contained = true;
					break;
				}
			}
			if (!contained && possibleFlyingSimulations.size() < maxFlyingSimulations) {
				possibleFlyingSimulations.add(simulation.reusableCopy());
			}
		}
		best = best.select(simulation, environment.position(), environment.verifiedLastPosition());
	}

	public Simulation bestSimulation() {
		return best;
	}

	public FPCSimulationContainer mergedWith(
		FPCSimulationContainer other,
		User user,
		SimulationEnvironment environment
	) {
		FPCSimulationContainer merged = new FPCSimulationContainer(maxFlyingSimulations);
		merged.best = best.select(other.best, environment.position(), environment.verifiedLastPosition());
		if (possibleFlyingSimulations != null) {
			for (Simulation simulation : possibleFlyingSimulations) {
				merged.add(simulation, user, environment);
			}
		}
		if (other.possibleFlyingSimulations != null) {
			for (Simulation simulation : other.possibleFlyingSimulations) {
				merged.add(simulation, user, environment);
			}
		}
		return merged;
	}

	public List<Simulation> possibleFlyingSimulations() {
		return possibleFlyingSimulations == null ? Collections.emptyList() : possibleFlyingSimulations;
	}

	public static FPCSimulationContainer def() {
		return new FPCSimulationContainer(8);
	}
}
