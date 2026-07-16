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
import de.jpx3.intave.math.MathHelper;
import de.jpx3.intave.share.BoundingBox;
import de.jpx3.intave.share.Position;

import static de.jpx3.intave.math.MathHelper.formatDouble;

public final class StaticWorldBorder implements WorldBorder {
	private final Position center;
	private final double size;
	private final int absoluteMaxSize;

	public StaticWorldBorder(Position center, double size, int absoluteMaxSize) {
		this.center = center;
		this.size = size;
		this.absoluteMaxSize = absoluteMaxSize;
	}

	private BlockShape collisionShape;

	@Override
	public BlockShape shape() {
		if (collisionShape == null) {
			double radius = size / 2.0;
			double minX = MathHelper.minmax(-absoluteMaxSize, center.getX() - radius - 1, absoluteMaxSize);
			double minZ = MathHelper.minmax(-absoluteMaxSize, center.getZ() - radius - 1, absoluteMaxSize);
			double maxX = MathHelper.minmax(-absoluteMaxSize, center.getX() + radius, absoluteMaxSize);
			double maxZ = MathHelper.minmax(-absoluteMaxSize, center.getZ() + radius, absoluteMaxSize);
			return collisionShape = BoundingBox.fromBounds(minX, Double.NEGATIVE_INFINITY, minZ, maxX, Double.POSITIVE_INFINITY, maxZ);
		}
		return collisionShape;
	}

	@Override
	public void tick() {

	}

	@Override
	public WorldBorder withCenterAt(Position center) {
		return new StaticWorldBorder(center, size, absoluteMaxSize);
	}

	@Override
	public WorldBorder withSize(double size) {
		return new StaticWorldBorder(center, size, absoluteMaxSize);
	}

	@Override
	public WorldBorder withLerpingSize(double fromSize, double toSize, long duration) {
		if (fromSize == toSize) {
			return new StaticWorldBorder(center, toSize, absoluteMaxSize);
		}
		return new MovingWorldBorder(center, fromSize, toSize, duration, 0, absoluteMaxSize);
	}

	@Override
	public WorldBorder withAbsoluteMaxSize(int absoluteMaxSize) {
		return new StaticWorldBorder(center, size, absoluteMaxSize);
	}

	@Override
	public String toString() {
		return "StaticWorldBorder{" +
			"center=" + center +
			", size=" + formatDouble(size, 2) +
			", absoluteMaxSize=" + absoluteMaxSize +
			'}';
	}
}
