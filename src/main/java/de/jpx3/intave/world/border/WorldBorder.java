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

package de.jpx3.intave.world.border;

import de.jpx3.intave.block.shape.BlockShape;
import de.jpx3.intave.share.Position;

public interface WorldBorder {
	BlockShape shape();
	void tick();

	WorldBorder withCenterAt(
		Position center
	);

	WorldBorder withSize(
		double size
	);

	WorldBorder withLerpingSize(
		double fromSize,
		double toSize,
		long duration
	);

	WorldBorder withAbsoluteMaxSize(int absoluteMaxSize);

	static WorldBorder createDefault() {
		return new StaticWorldBorder(
			Position.immutableEmpty(),
			6_000_000.0,
			29999984
		);
	}
}
