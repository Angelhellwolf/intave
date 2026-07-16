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

import de.jpx3.intave.IntaveControl;
import de.jpx3.intave.block.shape.BlockShape;
import de.jpx3.intave.block.shape.BlockShapes;
import org.bukkit.Material;

import java.util.Objects;


public final class BlockState {
	private static final BlockState EMPTY = new BlockState(BlockShapes.emptyShape(), BlockShapes.emptyShape(), Material.AIR, 0);
	private static final BlockState STONE = new BlockState(BlockShapes.originCube(), BlockShapes.originCube(), Material.STONE, 0);
	private final BlockShape outlineShape;
	private final BlockShape collisionShape;
	private final Material type;
	private final int variantIndex;
	private final long creation = System.currentTimeMillis();
	private int hashCode = 0;

	public BlockState(BlockShape outlineShape, BlockShape collisionShape, Material type, int variantIndex) {
		this.outlineShape = outlineShape;
		this.collisionShape = collisionShape;
		this.type = type;
		this.variantIndex = variantIndex;
	}

	/**
	 * Returns the bounding box of this block state.
	 *
	 * @return the bounding box of this block state
	 */
	public BlockShape outlineShape() {
		return outlineShape;
	}

	/**
	 * Retrieve the blocks bounding boxes
	 *
	 * @return the blocks bounding boxes
	 */
	public BlockShape collisionShape() {
		return collisionShape;
	}

	/**
	 * Retrieve the blocks type
	 *
	 * @return the blocks type
	 */
	public Material type() {
		return type;
	}

	/**
	 * Retrieve the blocks variant
	 *
	 * @return the blocks variant
	 */
	public int variantIndex() {
		return variantIndex;
	}

	/**
	 * Indicates if this entry effectively expired.
	 * Expiries neither have to be acknowledged nor followed - this only serves as a possible indicator
	 *
	 * @return whether the state is expired
	 */
	@Deprecated
	public boolean expired() {
		return !IntaveControl.IGNORE_CACHE_REFRESH_ON_SIMULATION_FAULT && age() > 10000;
	}

	long age() {
		return System.currentTimeMillis() - creation;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BlockState that = (BlockState) o;
		if (variantIndex != that.variantIndex) return false;
		if (creation != that.creation) return false;
		if (!Objects.equals(collisionShape, that.collisionShape)) return false;
		return type == that.type;
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			int result = collisionShape != null ? collisionShape.hashCode() : 0;
			result = 31 * result + (type != null ? type.hashCode() : 0);
			result = 31 * result + variantIndex;
			result = 31 * result + Long.hashCode(creation);
			hashCode = result;
		}
		return hashCode;
	}

	public static BlockState empty() {
		return EMPTY;
	}

	public static BlockState stone() {
		return STONE;
	}
}
