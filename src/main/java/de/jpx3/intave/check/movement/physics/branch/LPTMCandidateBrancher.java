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
import de.jpx3.intave.share.Motion;

import java.util.List;

// Last Post-Tick Motion
public final class LPTMCandidateBrancher extends MovementSearchBrancher {
	@Override
	public void branch(
		MovementSearchInput input,
		MovementSearchBranch inputBranch,
		List<MovementSearchBranch> outputBranches
	) {
		SimulationEnvironment environment = input.environment();
		List<Motion> candidates = environment.postTickMotionCandidates();
		if (candidates.isEmpty()) {
			outputBranches.add(inputBranch);
			return;
		}
		if (candidates.size() == 1) {
//			if (!candidates.get(0).equals(environment.mutableBaseMotionCopy())) {
//				System.err.println("candidates.size()=" + candidates.size() + " but candidate != base motion");
//				System.err.println("candidate=" + candidates.get(0));
//				System.err.println("base motion=" + environment.mutableBaseMotionCopy());
//				throw new IllegalStateException("Expected the only candidate to be equal to the base motion, but it was not");
//			}
			outputBranches.add(inputBranch);
			return;
		}
		for (Motion candidate : candidates) {
			MovementSearchBranch branch = inputBranch.modifyBefore(env -> {
				env.setBaseMotion(candidate);
				return env;
			});
			outputBranches.add(branch);
		}
	}
}
