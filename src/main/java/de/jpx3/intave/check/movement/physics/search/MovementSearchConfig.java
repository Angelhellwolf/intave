package de.jpx3.intave.check.movement.physics.search;

import de.jpx3.intave.check.movement.physics.MovementConfiguration;
import de.jpx3.intave.search.SearchConfig;

public final class MovementSearchConfig extends SearchConfig {
	private final MovementConfiguration configuration;

	private MovementSearchConfig(MovementConfiguration configuration) {
		this.configuration = configuration;
	}

	public static MovementSearchConfig blank(MovementSearchInput input) {
		return new MovementSearchConfig(MovementConfiguration.blank());
	}

	public MovementConfiguration moveConfig() {
		return configuration;
	}

	@Deprecated
	MovementSearchConfig withConfiguration(MovementConfiguration configuration) {
		return new MovementSearchConfig(configuration);
	}

	public MovementSearchConfig withHandActive(boolean handActive) {
		return new MovementSearchConfig(configuration.withHandActive(handActive));
	}

	public MovementSearchConfig withKeypress(int forward, int strafe) {
		return new MovementSearchConfig(
			configuration.withKeypress(forward, strafe)
		);
	}

	public MovementSearchConfig withReduceTicks(int ticks) {
		return new MovementSearchConfig(configuration.withReduceTicks(ticks));
	}

	public MovementSearchConfig withReduceBefore(boolean reduceBefore) {
		return new MovementSearchConfig(configuration.withReduceBefore(reduceBefore));
	}

	public MovementSearchConfig withJumped(boolean jumped) {
		return new MovementSearchConfig(configuration.withJumped(jumped));
	}

	public boolean isJumping() {
		return configuration.isJumping();
	}

	public MovementSearchConfig withSprintingSetTo(boolean sprinting) {
		return new MovementSearchConfig(configuration.withSprintingSetTo(sprinting));
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
		return this.configuration.equals(that.configuration);
	}

	@Override
	public int hashCode() {
		return this.configuration.hashCode();
	}
}
