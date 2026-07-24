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

import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.user.meta.ProtocolMetadata;

import java.util.List;

final class JumpBrancher extends MovementSearchBrancher {
  private static final boolean[] OPTIMISTIC = new boolean[]{true, false};
  private static final boolean[] PESSIMISTIC = new boolean[]{false, true};

  private final boolean restricted;

	JumpBrancher(boolean restricted) {
		this.restricted = restricted;
	}

	@Override
  public void branch(MovementSearchInput input, MovementSearchBranch inputBranch, List<MovementSearchBranch> outputBranches) {
    if (!input.jumpingBranchNecessary()) {
      outputBranches.add(inputBranch);
      return;
    }

    SimulationEnvironment environment = inputBranch.modifiedMutableView(input.environment());
    ProtocolMetadata protocol = input.user().meta().protocol();
    boolean estimatedJump = Math.abs(environment.offsetMotionY() - environment.jumpMotion()) < 0.0001;

    int writtenOutputBranches = 0;
    for (boolean jumped : estimatedJump ? OPTIMISTIC : PESSIMISTIC) {
      if (jumped && restricted && !environment.lastOnGround() && !environment.inLava() && !environment.inWater()) {
        continue;
      }
      if (jumped && environment.denyJump()) {
        continue;
      }
      if (!jumped && restricted && inputBranch.moveConfig().isSprinting() && environment.isSneaking() && !protocol.combatUpdate()) {
        continue;
      }
      writtenOutputBranches++;
      if (estimatedJump) {
        outputBranches.add(inputBranch.withPredictedJumped(jumped));
      } else {
        outputBranches.add(inputBranch.withJumped(jumped));
      }
    }
    if (writtenOutputBranches == 0) {
      outputBranches.add(inputBranch.withJumped(false));
    }
  }

  public static JumpBrancher restricted() {
    return new JumpBrancher(true);
  }

  public static JumpBrancher unrestricted() {
    return new JumpBrancher(false);
  }
}
