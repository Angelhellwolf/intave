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

import static de.jpx3.intave.check.movement.physics.MoveMetric.SPRINT_CHANGE;

final class SprintingBrancher extends MovementSearchBrancher {
  private static final boolean[] ALWAYS = new boolean[]{true};
  private static final boolean[] OPTIMISTIC = new boolean[]{true, false};
  private static final boolean[] PESSIMISTIC = new boolean[]{false, true};
  private static final boolean[] NEVER = new boolean[]{false};

  @Override
  public void branch(
    MovementSearchInput input, MovementSearchBranch inputBranch,
    List<MovementSearchBranch> outputBranches
  ) {
    if (!input.sprintingBranchNecessary()) {
      outputBranches.add(inputBranch);
      return;
    }

    SimulationEnvironment environment = input.environment();
    boolean[] selector = sprintSelector(input, environment);

    if (selector.length == 1) {
      outputBranches.add(inputBranch.withSprintingSetTo(selector[0]));
      return;
    }

    for (boolean sprinting : selector) {
      if (sprinting && input.user().meta().abilities().foodLevel < 6) {
        continue;
      }
      outputBranches.add(inputBranch.withSprintingSetTo(sprinting));
    }
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
