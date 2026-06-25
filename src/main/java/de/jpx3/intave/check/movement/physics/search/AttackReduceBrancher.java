package de.jpx3.intave.check.movement.physics.search;

import java.util.Set;

final class AttackReduceBrancher extends MovementSearchBrancher {
  private static final boolean[] PESSIMISTIC = new boolean[]{false, true};
  private static final boolean[] NEVER = new boolean[]{false};

  @Override
  public Set<MovementSearchConfig> branch(MovementSearchInput input, MovementSearchConfig config) {
    Set<MovementSearchConfig> result = ordered();
    for (int reduceIndex = 0; reduceIndex <= Math.min(input.environment().reduceTicks(), 3); reduceIndex++) {
      for (boolean reduceBefore : reduceIndex > 0 ? PESSIMISTIC : NEVER) {
        result.add(config.withReduceTicks(reduceIndex).withReduceBefore(reduceBefore));
      }
    }
    return result;
  }
}
