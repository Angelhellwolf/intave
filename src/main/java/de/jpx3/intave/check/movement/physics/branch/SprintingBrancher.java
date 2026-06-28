package de.jpx3.intave.check.movement.physics.branch;

import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.user.meta.ProtocolMetadata;

import java.util.Collections;
import java.util.Set;

import static de.jpx3.intave.check.movement.physics.MoveMetric.SPRINT_CHANGE;

final class SprintingBrancher extends MovementSearchBrancher {
  private static final boolean[] ALWAYS = new boolean[]{true};
  private static final boolean[] OPTIMISTIC = new boolean[]{true, false};
  private static final boolean[] PESSIMISTIC = new boolean[]{false, true};
  private static final boolean[] NEVER = new boolean[]{false};

  @Override
  public Set<MovementSearchConfig> branch(MovementSearchInput input, MovementSearchConfig config) {
    SimulationEnvironment environment = input.environment();
    boolean[] selector = sprintSelector(input, environment);

    if (selector.length == 1) {
      return Collections.singleton(config.withSprintingSetTo(selector[0]));
    }

    Set<MovementSearchConfig> result = ordered();
    for (boolean sprinting : selector) {
      if (sprinting && input.user().meta().abilities().foodLevel < 6) {
        continue;
      }
      result.add(config.withSprintingSetTo(sprinting));
    }
    return result;
  }

  private boolean[] sprintSelector(MovementSearchInput input, SimulationEnvironment environment) {
    ProtocolMetadata protocol = input.user().meta().protocol();
    if (protocol.combatUpdate()) {
      return environment.sprintingAllowed() || environment.hasSprintSpeed() ? PESSIMISTIC : NEVER;
    }
    boolean certain = environment.ticksPast(SPRINT_CHANGE) > 1;
    if (environment.isSprinting()) {
      return certain ? ALWAYS : OPTIMISTIC;
    }
    return certain ? NEVER : PESSIMISTIC;
  }
}
