package de.jpx3.intave.check.movement.physics.search;

import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.user.meta.ProtocolMetadata;

import java.util.Set;

final class JumpBrancher extends MovementSearchBrancher {
  private static final boolean[] OPTIMISTIC = new boolean[]{true, false};
  private static final boolean[] PESSIMISTIC = new boolean[]{false, true};

  @Override
  public Set<MovementSearchConfig> branch(MovementSearchInput input, MovementSearchConfig config) {
    SimulationEnvironment environment = input.environment();
    ProtocolMetadata protocol = input.user().meta().protocol();
    boolean estimatedJump = Math.abs(environment.motionY() - (1 - input.user().sizeOf(environment.pose()).height() % 1)) < 1e-5
      || Math.abs(environment.motionY() - environment.jumpMotion()) < 0.0001;

    Set<MovementSearchConfig> result = ordered();
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
    return result;
  }
}
