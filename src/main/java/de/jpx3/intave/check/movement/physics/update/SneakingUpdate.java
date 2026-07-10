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

package de.jpx3.intave.check.movement.physics.update;

import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.user.meta.MovementMetadata;

public final class SneakingUpdate extends TickAmbiguousUpdate {
	private final boolean sneaking;
	private final CausalConstraint constraint;

	private SneakingUpdate(boolean sneaking, CausalConstraint constraint) {
		this.sneaking = sneaking;
		this.constraint = constraint;
	}

	@Override
	public void applyTo(SimulationEnvironment environment) {
		environment.setSneaking(sneaking);
	}

	@Override
	public CausalConstraint constraint() {
		return constraint;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		SneakingUpdate that = (SneakingUpdate) obj;
		if (sneaking != that.sneaking) return false;
		return constraint.equals(that.constraint);
	}

	@Override
	public int hashCode() {
		int result = (sneaking ? 1 : 0);
		result = 31 * result + constraint.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "SneakingUpdate{" +
			"sneaking=" + sneaking +
			", constraint=" + constraint +
			'}';
	}

	public static SneakingUpdate sneaking(boolean sneaking, MovementMetadata metadata) {
		return new SneakingUpdate(sneaking, CausalConstraint.onlyThisTick(metadata));
	}
}
