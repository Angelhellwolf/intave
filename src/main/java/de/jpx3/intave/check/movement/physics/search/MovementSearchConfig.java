package de.jpx3.intave.check.movement.physics.search;

import de.jpx3.intave.check.movement.physics.MovementConfiguration;
import de.jpx3.intave.search.SearchConfig;
import de.jpx3.intave.share.Rotation;

import java.util.Objects;

public final class MovementSearchConfig extends SearchConfig {
	private final MovementConfiguration configuration;
	private final Rotation rotation;

	private MovementSearchConfig(MovementConfiguration configuration, Rotation rotation) {
		this.configuration = configuration;
		this.rotation = rotation;
	}

	public static MovementSearchConfig blank(MovementSearchInput input) {
		return new MovementSearchConfig(MovementConfiguration.blank(), null);
	}

	public MovementConfiguration moveConfig() {
		return configuration;
	}

	public Rotation rotation() {
		return rotation;
	}

	@Deprecated
	MovementSearchConfig withMoveConfig(MovementConfiguration configuration) {
		return new MovementSearchConfig(configuration, rotation);
	}

	public MovementSearchConfig withRotation(Rotation rotation) {
		return new MovementSearchConfig(configuration, rotation);
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
			Objects.equals(this.rotation, that.rotation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.configuration, this.rotation);
	}
}
