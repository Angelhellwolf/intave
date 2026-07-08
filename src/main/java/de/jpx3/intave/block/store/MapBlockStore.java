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

import de.jpx3.intave.share.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public final class MapBlockStore implements BlockStore {
	private final Map<Long, BlockState> map;

	private MapBlockStore(Map<Long, BlockState> map) {
		this.map = map;
	}

	@Override
	public BlockState get(int x, int y, int z) {
		return map.get(bigKey(x, y, z));
	}

	@Override
	public boolean put(int x, int y, int z, BlockState state) {
		map.put(bigKey(x, y, z), state);
		return true;
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public void removeIf(Predicate<BlockState> predicate) {
		map.entrySet().removeIf(entry -> predicate.test(entry.getValue()));
	}

	@Override
	public void clear() {
		map.clear();
	}

	private static long bigKey(int posX, int posY, int posZ) {
		return (posX & 0x3fffffL) << 42 | (posY & 0xfffffL) | (posZ & 0x3fffffL) << 20;
	}

	public static MapBlockStore of(Map<Long, BlockState> map) {
		return new MapBlockStore(map);
	}

	public static MapBlockStore ofHashMap() {
		return new MapBlockStore(new HashMap<>());
	}
}
