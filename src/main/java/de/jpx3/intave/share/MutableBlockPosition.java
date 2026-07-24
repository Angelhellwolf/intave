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

package de.jpx3.intave.share;

public final class MutableBlockPosition {
	private int x;
	private int y;
	private int z;

	public MutableBlockPosition() {
		this(0, 0, 0);
	}

	public MutableBlockPosition(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public MutableBlockPosition(BlockPosition position) {
		this(position.getX(), position.getY(), position.getZ());
	}

	public int x() {
		return this.x;
	}

	public int y() {
		return this.y;
	}

	public int z() {
		return this.z;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public void setZ(int z) {
		this.z = z;
	}

	public BlockPosition toBlockPosition() {
		return new BlockPosition(this.x, this.y, this.z);
	}

	@Override
	public int hashCode() {
		int result = Integer.hashCode(this.x);
		result = 31 * result + Integer.hashCode(this.y);
		result = 31 * result + Integer.hashCode(this.z);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		MutableBlockPosition other = (MutableBlockPosition) obj;
		return this.x == other.x && this.y == other.y && this.z == other.z;
	}
}
