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

package de.jpx3.intave.block.store;

import de.jpx3.intave.annotate.Nullable;
import de.jpx3.intave.share.BlockPosition;
import de.jpx3.intave.share.BlockState;

import java.util.function.Predicate;

public interface BlockStore {
	@Nullable BlockState get(int x, int y, int z);

	default @Nullable BlockState get(BlockPosition position) {
		return get(position.getX(), position.getY(), position.getZ());
	}

	boolean put(int x, int y, int z, @Nullable BlockState state);

	default boolean put(BlockPosition position, @Nullable BlockState state) {
		return put(position.getX(), position.getY(), position.getZ(), state);
	}

	default boolean remove(int x, int y, int z) {
		return put(x, y, z, null);
	}

	default boolean remove(BlockPosition position) {
		return remove(position.getX(), position.getY(), position.getZ());
	}

	int size();

	void removeIf(Predicate<BlockState> predicate);

	default void clear() {
		removeIf(state -> true);
	}

	default BlockStore withSynchronization() {
		return SynchronizedBlockStore.of(this);
	}
}
