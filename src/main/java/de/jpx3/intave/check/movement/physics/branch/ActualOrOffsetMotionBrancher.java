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

import java.util.List;

public final class ActualOrOffsetMotionBrancher extends MovementSearchBrancher {
	@Override
	public void branch(MovementSearchInput input, MovementSearchBranch inputBranch, List<MovementSearchBranch> outputBranches) {
		if (!input.actualMotionBranchNecessary()) {
			outputBranches.add(inputBranch);
			return;
		}

		MovementConfiguration config = inputBranch.moveConfig();
		outputBranches.add(inputBranch.withMoveConfig(config.allowOverrideToActualMotion(), "_allowOverrideToActualMotion"));
		outputBranches.add(inputBranch.withMoveConfig(config.denyOverrideToActualMotion(), "_denyOverrideToActualMotion"));
	}
}
