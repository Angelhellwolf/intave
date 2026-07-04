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

  @Override
  public void branch(MovementSearchInput input, MovementSearchConfig config, List<MovementSearchConfig> result) {
    SimulationEnvironment environment = input.environment();
    ProtocolMetadata protocol = input.user().meta().protocol();
    boolean estimatedJump = Math.abs(environment.motionY() - (1 - input.user().sizeOf(environment.pose()).height() % 1)) < 1e-5
      || Math.abs(environment.motionY() - environment.jumpMotion()) < 0.0001;

    for (boolean jumped : estimatedJump ? OPTIMISTIC : PESSIMISTIC) {
      if (jumped && !environment.lastOnGround() && !environment.inLava() && !environment.inWater()) {
        continue;
      }
      if (jumped && environment.denyJump()) {
        continue;
      }
      if (config.moveConfig().isSprinting() && environment.isSneaking() && !jumped && !protocol.combatUpdate()) {
        continue;
      }
      result.add(config.withJumped(jumped));
    }
  }
}
