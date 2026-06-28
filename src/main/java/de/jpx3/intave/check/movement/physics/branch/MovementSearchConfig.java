package de.jpx3.intave.check.movement.physics.branch;

import de.jpx3.intave.check.movement.physics.MovementConfiguration;
import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.search.SearchConfig;
import de.jpx3.intave.share.Rotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

public final class MovementSearchConfig extends SearchConfig {
	private final MovementConfiguration configuration;
	private final List<UnaryOperator<SimulationEnvironment>> environmentModifier;

	private MovementSearchConfig(MovementConfiguration configuration, List<UnaryOperator<SimulationEnvironment>> environmentModifier) {
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.environmentModifier = Objects.requireNonNull(environmentModifier, "environmentModifier");
	}

	public static MovementSearchConfig blank(MovementSearchInput input) {
		return new MovementSearchConfig(MovementConfiguration.blank(), new ArrayList<>());
	}

	public MovementConfiguration moveConfig() {
		return configuration;
	}

	@Deprecated
	MovementSearchConfig withMoveConfig(MovementConfiguration configuration) {
		return new MovementSearchConfig(configuration, environmentModifier);
	}

	public MovementSearchConfig withEnvironmentModifier(UnaryOperator<SimulationEnvironment> modifier) {
		List<UnaryOperator<SimulationEnvironment>> newList = new ArrayList<>(environmentModifier);
		newList.add(modifier);
		return new MovementSearchConfig(configuration, newList);
	}

	public MovementSearchConfig withRotation(Rotation rotation) {
		return withEnvironmentModifier(env -> {
			env.setRotation(rotation);
			return env;
		});
	}

	public SimulationEnvironment applyTo(SimulationEnvironment env) {
		for (UnaryOperator<SimulationEnvironment> modifier : environmentModifier) {
			env = modifier.apply(env);
		}
		return env;
	}

	public MovementSearchConfig withHandActive(boolean handActive) {
		return withMoveConfig(configuration.withHandActive(handActive));
	}

	public MovementSearchConfig withKeypress(int forward, int strafe) {
		return withMoveConfig(
			configuration.withKeypress(forward, strafe)
		);
	}

	public MovementSearchConfig withReduceTicks(int ticks) {
		return withMoveConfig(configuration.withReduceTicks(ticks));
	}

	public MovementSearchConfig withReduceBefore(boolean reduceBefore) {
		return withMoveConfig(configuration.withReduceBefore(reduceBefore));
	}

	public MovementSearchConfig withJumped(boolean jumped) {
		return withMoveConfig(configuration.withJumped(jumped));
	}

	public boolean isJumping() {
		return configuration.isJumping();
	}

	public MovementSearchConfig withSprintingSetTo(boolean sprinting) {
		return withMoveConfig(configuration.withSprintingSetTo(sprinting));
	}

	public boolean isSprinting() {
		return configuration.isSprinting();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MovementSearchConfig)) {
			return false;
		}
		MovementSearchConfig that = (MovementSearchConfig) other;
		return this.configuration.equals(that.configuration) &&
			Objects.equals(this.environmentModifier, that.environmentModifier);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.configuration, this.environmentModifier);
	}
}
