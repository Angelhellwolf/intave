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

import de.jpx3.intave.check.movement.physics.config.MovementConfiguration;
import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.share.Rotation;

import java.util.Objects;
import java.util.function.UnaryOperator;

public final class MovementSearchBranch {
	private final static MovementSearchBranch BLANK = new MovementSearchBranch(MovementConfiguration.blank(), env -> env, true, "");
	private final MovementConfiguration configuration;
	private final UnaryOperator<SimulationEnvironment> environmentModifier;
	private final boolean canFinishExplicitTick;
	private final String branchIdentifier;

	private MovementSearchBranch(MovementConfiguration configuration, UnaryOperator<SimulationEnvironment> environmentModifier, boolean canFinishExplicitTick, String branchIdentifier) {
		this.configuration = configuration;
		this.environmentModifier = environmentModifier;
		this.canFinishExplicitTick = canFinishExplicitTick;
		this.branchIdentifier = branchIdentifier;
	}

	public static MovementSearchBranch blank(MovementSearchInput input) {
		return BLANK;
	}

	public MovementConfiguration moveConfig() {
		return configuration;
	}

	MovementSearchBranch withMoveConfig(MovementConfiguration configuration, String branchAddition) {
		return new MovementSearchBranch(configuration, environmentModifier, canFinishExplicitTick, branchIdentifier + branchAddition);
	}

	public MovementSearchBranch modifyBefore(UnaryOperator<SimulationEnvironment> modifier) {
		return new MovementSearchBranch(
			configuration,
			andThen(modifier, environmentModifier),
			canFinishExplicitTick,
			branchIdentifier + "_beforeModifier"
		);
	}

	public MovementSearchBranch modifyAfter(UnaryOperator<SimulationEnvironment> modifier, String branchAddition) {
		return new MovementSearchBranch(
			configuration,
			andThen(environmentModifier, modifier),
			canFinishExplicitTick,
			branchIdentifier + branchAddition
		);
	}

	public String branchIdentifier() {
		return branchIdentifier;
	}

	private static <T> UnaryOperator<T> andThen(UnaryOperator<T> first, UnaryOperator<T> second) {
		return t -> second.apply(first.apply(t));
	}

	public MovementSearchBranch withExplicitTickFinishAllow(boolean canFinishUserTick) {
		boolean newFinishTick = canFinishUserTick && canFinishExplicitTick;
		return new MovementSearchBranch(
			configuration, environmentModifier, newFinishTick, branchIdentifier + "_explicitTickFinishAllow=" + newFinishTick
		);
	}

	public MovementSearchBranch setToOnlyUsableForImplicitFlyingPackets() {
		return withExplicitTickFinishAllow(false);
	}

	public MovementSearchBranch withRotation(Rotation rotation) {
		return modifyAfter(env -> {
			env = env.mutableView();
			env.setRotation(rotation);
			return env;
		}, "_rotation");
	}

	public SimulationEnvironment modifiedMutableView(SimulationEnvironment env) {
		return environmentModifier.apply(env.mutableView());
	}

	public MovementSearchBranch withHandActive(boolean handActive) {
		return withMoveConfig(configuration.withHandActive(handActive), "_handActive=" + handActive);
	}

	public MovementSearchBranch withKeypress(int forward, int strafe) {
		return withMoveConfig(
			configuration.withKeypress(forward, strafe),
			"_keypress=" + forward + "," + strafe
		);
	}

	public MovementSearchBranch withPredictedKeypress(int forward, int strafe) {
		return withMoveConfig(
			configuration.withKeypress(forward, strafe),
			"_predictedKeypress=" + forward + "," + strafe
		);
	}

	public MovementSearchBranch withReduceTicks(int ticks) {
		return withMoveConfig(configuration.withReduceTicks(ticks), "_reduceTicks=" + ticks);
	}

	public MovementSearchBranch withReduceBefore(boolean reduceBefore) {
		return withMoveConfig(configuration.withReduceBefore(reduceBefore), "_reduceBefore=" + reduceBefore);
	}

	public MovementSearchBranch withJumped(boolean jumped) {
		return withMoveConfig(configuration.withJumped(jumped), "_jumped=" + jumped);
	}

	public MovementSearchBranch withPredictedJumped(boolean jumped) {
		return withMoveConfig(configuration.withJumped(jumped), "_predictedJump=" + jumped);
	}

	public boolean canFinishExplicitTick() {
		return canFinishExplicitTick;
	}

	public boolean isJumping() {
		return configuration.isJumping();
	}

	public MovementSearchBranch withSprintingSetTo(boolean sprinting) {
		return withMoveConfig(configuration.withSprintingSetTo(sprinting), "_sprinting=" + sprinting);
	}

	public MovementSearchBranch withPredictedSprintingSetTo(boolean sprinting) {
		return withMoveConfig(configuration.withSprintingSetTo(sprinting), "_predictedSprinting=" + sprinting);
	}

	public boolean isSprinting() {
		return configuration.isSprinting();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MovementSearchBranch)) {
			return false;
		}
		MovementSearchBranch that = (MovementSearchBranch) other;
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
