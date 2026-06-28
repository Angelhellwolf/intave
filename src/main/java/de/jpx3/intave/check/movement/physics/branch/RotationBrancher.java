package de.jpx3.intave.check.movement.physics.branch;

import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;

import java.util.Set;

public final class RotationBrancher extends MovementSearchBrancher {
	@Override
	public Set<MovementSearchConfig> branch(MovementSearchInput input, MovementSearchConfig config) {
		SimulationEnvironment environment = input.environment();
		if (environment.lastRotation().equals(environment.rotation())) {
			return single(config);
		}
		Set<MovementSearchConfig> ordered = ordered();
		ordered.add(config);
		ordered.add(config.withRotation(environment.lastRotation()));
		return ordered;
	}
}
