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

import java.util.Objects;

import static de.jpx3.intave.math.MathHelper.formatDouble;

public final class MovingWorldBorder implements WorldBorder {
	private final Position center;
	private final double fromSize, toSize;
	private final long duration;
	private long countedDuration;
	private final int absoluteMaxSize;

	public MovingWorldBorder(
		Position center,
		double fromSize, double toSize,
		long duration, long countedDuration,
		int absoluteMaxSize
	) {
		this.center = center;
		this.fromSize = fromSize;
		this.toSize = toSize;
		this.duration = duration;
		this.countedDuration = countedDuration;
		this.absoluteMaxSize = absoluteMaxSize;
	}

	private BlockShape collisionShape;

	@Override
	public BlockShape shape() {
		if (collisionShape == null) {
			double radius = size() / 2.0;
			double minX = MathHelper.minmax(-absoluteMaxSize, center.getX() - radius - 1, absoluteMaxSize);
			double minZ = MathHelper.minmax(-absoluteMaxSize, center.getZ() - radius - 1, absoluteMaxSize);
			double maxX = MathHelper.minmax(-absoluteMaxSize, center.getX() + radius, absoluteMaxSize);
			double maxZ = MathHelper.minmax(-absoluteMaxSize, center.getZ() + radius, absoluteMaxSize);
			return collisionShape = BoundingBox.fromBounds(minX, Double.NEGATIVE_INFINITY, minZ, maxX, Double.POSITIVE_INFINITY, maxZ);
		}
		return collisionShape;
	}

	private double size() {
		double d0 = (double) countedDuration / duration;
		return d0 < 1.0 ? fromSize + (toSize - fromSize) * d0 : toSize;
	}

	@Override
	public void tick() {
		countedDuration += 50;
	}

	@Override
	public WorldBorder withCenterAt(Position center) {
		return new MovingWorldBorder(center, fromSize, toSize, duration, countedDuration, absoluteMaxSize);
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
		return new MovingWorldBorder(center, fromSize, toSize, duration, countedDuration, absoluteMaxSize);
	}

	@Override
	public int hashCode() {
		int result = center != null ? center.hashCode() : 0;
		result = 31 * result + Double.hashCode(fromSize);
		result = 31 * result + Double.hashCode(toSize);
		result = 31 * result + Long.hashCode(duration);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		MovingWorldBorder that = (MovingWorldBorder) obj;
		if (Double.compare(that.fromSize, fromSize) != 0) return false;
		if (Double.compare(that.toSize, toSize) != 0) return false;
		if (duration != that.duration) return false;
		return Objects.equals(center, that.center);
	}

	@Override
	public String toString() {
		return "MovingWorldBorder{" +
			"center=" + center +
			", fromSize=" + formatDouble(fromSize, 2) +
			", toSize=" + formatDouble(toSize, 2) +
			", duration=" + duration +
			", absoluteMaxSize=" + absoluteMaxSize +
			'}';
	}
}
