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

package de.jpx3.intave.check.movement.physics.branch;

import de.jpx3.intave.check.movement.physics.MovementConfiguration;
import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.share.Rotation;

import java.util.Objects;
import java.util.function.UnaryOperator;

public final class MovementSearchConfig {
	private final MovementConfiguration configuration;
	private final UnaryOperator<SimulationEnvironment> environmentModifier;
	private final boolean canFinishExplicitTick;

	private MovementSearchConfig(MovementConfiguration configuration, UnaryOperator<SimulationEnvironment> environmentModifier, boolean canFinishExplicitTick) {
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.environmentModifier = Objects.requireNonNull(environmentModifier, "environmentModifier");
		this.canFinishExplicitTick = canFinishExplicitTick;
	}

	public static MovementSearchConfig blank(MovementSearchInput input) {
		return new MovementSearchConfig(MovementConfiguration.blank(), env -> env, true);
	}

	public MovementConfiguration moveConfig() {
		return configuration;
	}

	@Deprecated
	MovementSearchConfig withMoveConfig(MovementConfiguration configuration) {
		return new MovementSearchConfig(configuration, environmentModifier, canFinishExplicitTick);
	}

	public MovementSearchConfig withEnvironmentModifier(UnaryOperator<SimulationEnvironment> modifier) {
		return new MovementSearchConfig(configuration, modifier, canFinishExplicitTick);
	}

	public MovementSearchConfig withExplicitTickFinishAllow(boolean canFinishUserTick) {
		boolean newFinishTick = canFinishUserTick && canFinishExplicitTick;
		return new MovementSearchConfig(
			configuration, environmentModifier, newFinishTick
		);
	}

	public MovementSearchConfig withRotation(Rotation rotation) {
		return withEnvironmentModifier(env -> {
			env.setRotation(rotation);
			return env;
		});
	}

	public SimulationEnvironment applyTo(SimulationEnvironment env) {
		return environmentModifier.apply(env);
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

	public boolean canFinishExplicitTick() {
		return canFinishExplicitTick;
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
		int result = configuration.hashCode();
		result = 31 * result + environmentModifier.hashCode();
		return result;
	}
}
