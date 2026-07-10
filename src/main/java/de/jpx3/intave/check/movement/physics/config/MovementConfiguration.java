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

package de.jpx3.intave.check.movement.physics.config;

public interface MovementConfiguration {
	int forward();

	boolean isHandActive();

	boolean isJumping();

	boolean isReducing();

	boolean isSprinting();

	MovementConfiguration pressingA();

	MovementConfiguration pressingD();

	MovementConfiguration pressingS();

	MovementConfiguration pressingW();

	boolean reduceBefore();

	int reduceTicks();

	int strafe();

	MovementConfiguration withActiveHand();

	MovementConfiguration withForward(int forward);

	MovementConfiguration withHandActive(boolean hasHandActive);

	MovementConfiguration withJump();

	MovementConfiguration withJumped(boolean hasJumped);

	MovementConfiguration withKeypress(int forward, int strafe);

	MovementConfiguration withReduceBefore(boolean hasReduceBefore);

	MovementConfiguration withReduceTicks(int ticks);

	MovementConfiguration withSprinting();

	MovementConfiguration withSprintingSetTo(boolean sprinting);

	MovementConfiguration withStrafe(int strafe);

	MovementConfiguration withoutActiveHand();

	MovementConfiguration withoutJump();

	MovementConfiguration withoutKeypress();

	MovementConfiguration withoutReducing();

	MovementConfiguration withoutSprinting();

	TraceImmutableMovementConfiguration withRecording();

	static MovementConfiguration blank() {
		return IndexBasedMovementConfiguration.blank();
	}

	default String keysToString() {
		StringBuilder builder = new StringBuilder();
		int forward = forward();
		int strafe = strafe();
		if (forward == 1) {
			builder.append("W");
		} else if (forward == -1) {
			builder.append("S");
		}
		if (strafe == 1) {
			builder.append("D");
		} else if (strafe == -1) {
			builder.append("A");
		}
		return builder.toString();
	}

	default String toCompactString() {
		return ("(" + keysToString() + ") " +
			(isReducing() ? "_RED" + reduceTicks() : "") +
			(isSprinting() ? "_SPR" : "") +
			(isJumping() ? "_JMP" : "") +
			(isHandActive() ? "_HA" : "")
		).trim();
	}
}
